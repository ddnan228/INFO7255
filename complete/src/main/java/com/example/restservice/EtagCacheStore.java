package com.example.restservice;

import java.util.HashMap;

public class EtagCacheStore {
    private static HashMap<String, String> etagCache = new HashMap<>();
    private static HashMap<String, String> keyToEtag = new HashMap<>();

    public void putIntoCache(String etag, String jsonBody) {
        etagCache.put(etag, jsonBody);
    }

    public boolean containsEtag(String etag) {
        return etagCache.containsKey(etag);
    }

    public String getPlanFromCache(String etag) {
        return etagCache.get(etag);
    }

    public void mapKeyToEtag(String key, String etag) {
        keyToEtag.put(key, etag);
    }

    public String getEtagByKey(String key) {
        return keyToEtag.get(key);
    }

    public boolean keyHasEtag(String key) {
        return keyToEtag.containsKey(key);
    }

    public void removePlanFromCacheByKey(String key) {
        if(keyToEtag.containsKey(key)) {
            String tag = keyToEtag.get(key);
            keyToEtag.remove(key);
            if(etagCache.containsKey(tag)) {
                etagCache.remove(tag);
            }
            System.out.println("Plan removed from cache");
        }
    }


}
