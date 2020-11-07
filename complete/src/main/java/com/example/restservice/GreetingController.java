package com.example.restservice;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
// import javax.servlet.http.HttpServletResponse;

import com.example.restservice.authClient.AuthClient;
import com.example.restservice.etagUtils.EtagCacheStore;
import com.example.restservice.jsonUtils.JsonMapService;
import com.example.restservice.jsonUtils.JsonValidator;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
// import org.json.simple.JSONObject;
// import java.io.IOException;
// import com.github.fge.jsonschema.core.exceptions.ProcessingException;

// import org.springframework.web.filter.ShallowEtagHeaderFilter;

@RestController
public class GreetingController {
	Jedis jedis = JedisClient.getJedisClient();
	EtagCacheStore etagCacheStore = new EtagCacheStore();
	AuthClient authClient = new AuthClient();

	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	@RequestMapping("/greeting")
	public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
		return new Greeting(counter.incrementAndGet(), String.format(template, name));
	}

	@GetMapping("/token")
	public ResponseEntity<String> getToken() {
		try {
			String token = authClient.getToken();
			return new ResponseEntity<String>("This token will be expired in 60 mins: " + token, HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>("Cannot get token with exception " + e, HttpStatus.BAD_REQUEST);
		}
	}

	// get plan json from redis if existed
	@GetMapping("/plan/{id}")
	public ResponseEntity<String> read(@PathVariable(name="id") String id, @RequestHeader HttpHeaders requestHeader) {

		// Auth
		if(!authClient.validateToken(requestHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new JSONObject().put("Authentication Error: ", "No token or Invalid token").toString());
		}

		// process etag
		List<String> etag =  requestHeader.getIfNoneMatch();
		if(!etag.isEmpty() && etagCacheStore.containsEtag(etag.get(0))) {
			//System.out.println(etag.get(0));
			String plan = etagCacheStore.getPlanFromCache(etag.get(0));
			//System.out.println(plan);
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
	public ResponseEntity<String> create(@RequestBody String body, @RequestHeader HttpHeaders requestHeader) {
		try {
			// Auth
			if(!authClient.validateToken(requestHeader)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new JSONObject().put("Authentication Error: ", "No token or Invalid token").toString());
			}

			boolean isValid = JsonValidator.validateJson(body);
			if (isValid) {
				JSONObject json = new JSONObject(body);
				String key = json.get("objectId").toString();
				System.out.println("Create key-value.");
				jedis.connect();
				jedis.set(key, body);

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				String etag = etagCacheStore.putInAndGenerateEtage(key, body);
				headers.setETag(etag);

				JSONObject res = new JSONObject();
				res.put("objectID", json.get("objectId").toString());
				res.put("objectType", json.get("objectType").toString());

				return new ResponseEntity<>(res.toString(), headers, HttpStatus.ACCEPTED);
			} else {
				return new ResponseEntity<>("Json is not valid for plan schema", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			return new ResponseEntity<>("Json is not valid for plan schema", HttpStatus.BAD_REQUEST);
		}

	}

	@PutMapping("/plan/{id}")
	public ResponseEntity<String> update(@PathVariable(name="id") String id, @RequestBody String body, @RequestHeader HttpHeaders requestHeader) {
		// Auth
		if(!authClient.validateToken(requestHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new JSONObject().put("Authentication Error: ", "No token or Invalid token").toString());
		}

		// process the etag and remove it from etagStore
		List<String> etag =  requestHeader.getIfMatch();
		if(!etag.isEmpty() && etagCacheStore.containsEtag(etag.get(0))) {
			etagCacheStore.removePlanFromCacheByKey(id);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			System.out.println("Read value by key.");
			jedis.connect();
			String plan = jedis.get(id);
			if(plan == null || plan.isEmpty()) {
				return new ResponseEntity<>("Plan not found, please check the object_id or use post to create new plan.", headers, HttpStatus.NOT_FOUND);
			}

			// update plan with id, but check id matches in json first!!

			boolean validateID = JsonMapService.validateObjectId(id, body);
			if(!validateID) {
				return new ResponseEntity<>("Plan Object_id has changed, which is not allowed. Please use post to create new plan.", headers, HttpStatus.BAD_REQUEST);
			}
			try {
				boolean isValid = JsonValidator.validateJson(body);
				if (isValid) {
					JSONObject json = new JSONObject(body);
					System.out.println("put key-value.");
					jedis.connect();
					jedis.set(id, body);
					// set new Etag
					String newEtag = etagCacheStore.putInAndGenerateEtage(id, body);
					headers.setETag(newEtag);

					return new ResponseEntity<>("Plan has updated(put) with key " + id, headers, HttpStatus.ACCEPTED);
				} else {
					return new ResponseEntity<>("Json is not valid for plan schema", HttpStatus.BAD_REQUEST);
				}
			} catch (Exception e) {
				return new ResponseEntity<>("Json is not valid for plan schema", HttpStatus.BAD_REQUEST);
			}
		} else {
			return new ResponseEntity<>("Please provide the correct etag.", HttpStatus.BAD_REQUEST);
		}
	}

	@PatchMapping("/plan/{id}")
	public ResponseEntity<String> patchUpdate(@PathVariable(name="id") String id, @RequestBody String body, @RequestHeader HttpHeaders requestHeader) {
		// Auth
		if(!authClient.validateToken(requestHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new JSONObject().put("Authentication Error: ", "No token or Invalid token").toString());
		}

		// process the etag and remove it from etagStore
		List<String> etag =  requestHeader.getIfMatch();
		if(!etag.isEmpty() && etagCacheStore.containsEtag(etag.get(0))) {
			etagCacheStore.removePlanFromCacheByKey(id);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			System.out.println("start patch");
			jedis.connect();
			String plan = jedis.get(id);
			if(plan == null || plan.isEmpty()) {
				return new ResponseEntity<>("Plan not found, please check the object_id or use post to create new plan.", headers, HttpStatus.NOT_FOUND);
			}

			// update plan with id, but check id matches in json first!!

			boolean validateID = JsonMapService.validateObjectId(id, body);
			if(!validateID) {
				return new ResponseEntity<>("Plan Object_id has changed, which is not allowed. Please use post to create new plan.", headers, HttpStatus.BAD_REQUEST);
			}
			try {
				boolean isValid = JsonValidator.validateJson(body);
				if (isValid) {
					System.out.println("doing patch");
					jedis.connect();
					String patchedPlan = JsonMapService.patchJson(plan, body);
					jedis.set(id, patchedPlan);
					// set new Etag
					String newEtag = etagCacheStore.putInAndGenerateEtage(id, patchedPlan);
					headers.setETag(newEtag);

					return new ResponseEntity<>("Plan has updated(patch) with key " + id, headers, HttpStatus.ACCEPTED);
				} else {
					return new ResponseEntity<>("Json is not valid for plan schema", HttpStatus.BAD_REQUEST);
				}
			} catch (Exception e) {
				return new ResponseEntity<>("Json is not valid for plan schema", HttpStatus.BAD_REQUEST);
			}
		} else {
			return new ResponseEntity<>("Please provide the correct etag.", HttpStatus.BAD_REQUEST);
		}
	}

	// Delete a plan by key
	@DeleteMapping("/plan/{id}")
	public ResponseEntity<String> delete(@PathVariable(name="id") String id, @RequestHeader HttpHeaders requestHeader) {
		// Auth
		if(!authClient.validateToken(requestHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new JSONObject().put("Authentication Error: ", "No token or Invalid token").toString());
		}

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
