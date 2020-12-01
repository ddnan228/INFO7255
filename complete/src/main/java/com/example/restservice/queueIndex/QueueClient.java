package com.example.restservice.queueIndex;
import redis.clients.jedis.Jedis;
import com.example.restservice.storeService.JedisClient;
import org.json.JSONObject;

import java.util.Deque;
import java.util.LinkedList;

public class QueueClient {
    private static Deque<PlanMessage> indexQueue = new LinkedList<>();

    // push a plan into index queue
    public static void pushToQueue(PlanMessage message) {
        // message is a json string including id, isdelete, json body of plan
        try {
            indexQueue.offerLast(message);

            System.out.println("INFO: plan pushed to index queue successfully: " + message.id);
        } catch (Exception e) {
            System.out.println("Error: exception in push plan to index queue: " + e);
        }
    }

    public static PlanMessage getMessageForIndexer(){
        if(indexQueue.isEmpty()) {
            return null;
        } else {
            return indexQueue.pollFirst();
        }
    }
}
