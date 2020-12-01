package com.example.restservice.storeService;

import redis.clients.jedis.Jedis;

public class JedisClient {
    private static Jedis myJedisClient = new Jedis();

    public static Jedis getJedisClient() {
        return myJedisClient;
    }

    public void setKeyValuePair(String key, String value) {
        try {
            myJedisClient.connect();
            myJedisClient.set(key, value);
        } catch (Exception e) {
            System.out.println("Failed in setKeyValuePair to Jedis, " + e);
        }
    }

    public boolean checkIfKeyExist(String key) {
        try {
            myJedisClient.connect();
            return myJedisClient.exists(key);
        } catch (Exception e) {
            System.out.println("Failed in checkIfKeyExist to Jedis, " + e);
            return false;
        }
    }

    public String getValueByKey (String key) {
        try {
            myJedisClient.connect();
            return myJedisClient.get(key);
        } catch (Exception e) {
            System.out.println("Failed in checkIfKeyExist to Jedis, " + e);
            return "";
        }
    }

    public void hashSet(String key, String field, String value ) {
        try {
            myJedisClient.connect();
            myJedisClient.hset(key, field, value);
        } catch (Exception e) {
            System.out.println("Failed in hashSet to Jedis, " + e);
        }
    }
}