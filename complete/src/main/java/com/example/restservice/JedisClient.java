package com.example.restservice;

import redis.clients.jedis.Jedis;

public class JedisClient {
    private static Jedis myJedisClient = new Jedis();

    public static Jedis getJedisClient() {
        return myJedisClient;
    }
}