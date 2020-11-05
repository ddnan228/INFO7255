package com.example.restservice;

import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
// import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import javax.xml.bind.DatatypeConverter;
// import org.json.simple.JSONObject;
// import java.io.IOException;
// import com.github.fge.jsonschema.core.exceptions.ProcessingException;

// import org.springframework.web.filter.ShallowEtagHeaderFilter;

@RestController
public class GreetingController {
	Jedis jedis = JedisClient.getJedisClient();
	EtagCacheStore etagCacheStore = new EtagCacheStore();

	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	@RequestMapping("/greeting")
	public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
		return new Greeting(counter.incrementAndGet(), String.format(template, name));
	}

	// get plan json from redis if existed
	@GetMapping("/plan/{id}")
	public ResponseEntity<String> read(@PathVariable(name="id") String id, @RequestHeader HttpHeaders requestHeader) {
		// process etag
		List<String> etag =  requestHeader.getIfNoneMatch();
		if(!etag.isEmpty() && etagCacheStore.containsEtag(etag.get(0))) {
			System.out.println(etag.get(0));
			String plan = etagCacheStore.getPlanFromCache(etag.get(0));
			System.out.println(plan);
			HttpHeaders headers = new HttpHeaders();
			headers.setETag(etag.get(0));
			headers.setContentType(MediaType.APPLICATION_JSON);
			return new ResponseEntity<>(plan, headers, HttpStatus.NOT_MODIFIED);
		} else {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			System.out.println("Read value by key.");
			jedis.connect();
			String plan = jedis.get(id);
			if(etagCacheStore.keyHasEtag(id)) {
				headers.setETag(etagCacheStore.getEtagByKey(id));
			}
			if (plan != null)
				return new ResponseEntity<>(plan, headers, HttpStatus.ACCEPTED);
			else
				return new ResponseEntity<>("Plan not found", headers, HttpStatus.NOT_FOUND);
		}
	}

	// Create a plan by post a json body
	@PostMapping("/plan")
	public ResponseEntity<String> create(@RequestBody String body) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			// generate MD5 hashcode as etag
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(body.getBytes());
			byte[] digest = md.digest();
			String myHash = DatatypeConverter.printHexBinary(digest).toUpperCase();
			String etag = "\"" + myHash + "\"";
			etagCacheStore.putIntoCache(etag, body);
			headers.setETag(etag);

			boolean isValid = JsonValidator.validateJson(body);
			if (isValid) {
				jedis.connect();
				String key = UUID.randomUUID().toString();
				System.out.println("Create key-value.");
				jedis.set(key, body);
				etagCacheStore.mapKeyToEtag(key, etag);
				return new ResponseEntity<>("Plan has stored with key " + key, headers, HttpStatus.ACCEPTED);
			} else {
				return new ResponseEntity<>("Json is not valid for plan schema", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			return new ResponseEntity<>("Json is not valid for plan schema", HttpStatus.BAD_REQUEST);
		}

	}

	// Delete a plan by key
	@DeleteMapping("/plan/{id}")
	public ResponseEntity<String> delete(@PathVariable(name="id") String id) {
		jedis.connect();
		boolean existed = jedis.exists(id);
		if(existed) {
			jedis.del(id);
			etagCacheStore.removePlanFromCacheByKey(id);
			return new ResponseEntity<>("Plan with key " + id + " has been deleted.", HttpStatus.ACCEPTED);
		}
		else
			return new ResponseEntity<>("Plan not found", HttpStatus.NOT_FOUND);
	}

}
