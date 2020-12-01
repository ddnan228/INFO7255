package com.example.restservice;

import com.example.restservice.queueIndex.ElasticClient;
import com.example.restservice.storeService.JedisClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Paths;

import redis.clients.jedis.Jedis;

/*
* start redis server:    $redis-server
* start elastic search:  bin/elasticsearch
* start Kibana:  bin/kibana
* elastic search console: http://localhost:5601/app/dev_tools#/console
* */


@SpringBootApplication
public class RestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestServiceApplication.class, args);
        new RestServiceApplication().setSchemaToJedis();
        new ElasticClient().startIndexingEngine();
    }
/*
    @Bean
    ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        return new ShallowEtagHeaderFilter();
    }
*/
    private void setSchemaToJedis() {
        String schemaPath = "/Users/doudounan/Documents/INFO7255/gs-rest-service/complete/src/planSchema";
        String schema = "";
        try {
            schema = readJsonFile(schemaPath);
        } catch (Exception e) {
            System.out.println(e);
        }

        Jedis jedis = JedisClient.getJedisClient();
        try {
            jedis.set("planKey", schema);
            System.out.println("Plan schema has been set");
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Don't forget to start the redis server using $redis-server");
        }

    }

    private String readJsonFile(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

}
