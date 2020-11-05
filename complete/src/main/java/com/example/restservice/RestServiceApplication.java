package com.example.restservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import redis.clients.jedis.Jedis;


@SpringBootApplication
public class RestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestServiceApplication.class, args);
        new RestServiceApplication().setSchemaToJedis();
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
