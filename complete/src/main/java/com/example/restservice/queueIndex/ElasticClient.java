package com.example.restservice.queueIndex;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.Iterator;

public class ElasticClient {
    private static RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(new HttpHost("localhost", 9200, "http")));

    // start the elastic indexing pop engine
    public void startIndexingEngine() {
        try {
            System.out.println("The elastic search engine starts to pop the index queue");
            while(true) {
                PlanMessage message = QueueClient.getMessageForIndexer();
                if(message == null) {
                    continue;
                } else {
                    // parsing json body and using rest api put into elastic search
                    String id = message.id;
                    System.out.println("INFO: indexer start to indexing plan with id: " + id);
                    String jsonBody = message.jsonBody;
                    if(message.isDelete) {
                        // delete docs
                        deleteDocs(jsonBody);
                    } else {
                        // create docs
                        createDocs(jsonBody);
                    }

                }
            }
        } catch (Exception e) {
            System.out.println("Error: exception in elastic index engine at popping queue: " + e);
        }
    }


    private void createDocs(String jsonBody) {
        JSONObject result = new JSONObject(jsonBody);
        mappingObject(result, "");
    }

    // PUT docs, from parent to child
    private void mappingObject(JSONObject object, String parentId) {
        String currId = (String)object.get("objectId");
        JSONObject postBody = new JSONObject();
        Iterator<?> keys = object.keySet().iterator();
        while(keys.hasNext()) {
            String key = (String)keys.next();
            if(object.get(key) instanceof String) {
                postBody.put(key, object.get(key));
            } else if(object.get(key) instanceof Integer) {
                postBody.put(key, object.get(key));
            }
        }

        JSONObject plan_service = new JSONObject();
        if(parentId.length() == 0) {
            // current is parent
            String name = (String)object.get("objectType");
            plan_service.put("name", name);
            postBody.put("plan_service", plan_service);
            String json = postBody.toString();
            System.out.println("INFO: create parent json: " + json);
            putParentDoc(currId, json);
        } else {
            plan_service.put("parent", parentId);
            String name = (String)object.get("objectType");
            plan_service.put("name", name);
            postBody.put("plan_service", plan_service);
            String json = postBody.toString();
            System.out.println("INFO: create child json: " + json);
            putChildDoc(currId, parentId, json);
        }

        // mapping child object
        Iterator<?> keys2 = object.keySet().iterator();
        while(keys2.hasNext()) {
            String key = (String)keys2.next();
            if(object.get(key) instanceof JSONObject) {
                mappingObject((JSONObject)object.get(key), currId);
            } else if(object.get(key) instanceof JSONArray) {
                JSONArray array = (JSONArray)object.get(key);
                int len = array.length();
                for(int i = 0; i < len; i++) {
                    mappingObject(array.getJSONObject(i), currId);
                }
            }
        }
    }

    private void putParentDoc(String id, String json) {
        IndexRequest request = new IndexRequest("plan");
        request.id(id);
        request.source(json, XContentType.JSON);
        try {
            IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            System.out.println("Error in put parent doc into es: " + e + " for " + json);
        }

    }

    private void putChildDoc(String id, String parentId, String json) {
        IndexRequest request = new IndexRequest("plan");
        request.id(id);
        request.routing(parentId);
        request.source(json, XContentType.JSON);

        try {
            IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            System.out.println("Error in put child doc into es: " + e + " for " + json);
        }
    }

    private void deleteDocs(String jsonBody) {
        JSONObject result = new JSONObject(jsonBody);
        deleteObject(result);
    }

    // delete docs, from child to parent
    private void deleteObject(JSONObject object) {
        String currId = (String)object.get("objectId");
        JSONObject postBody = new JSONObject();
        Iterator<?> keys = object.keySet().iterator();
        while(keys.hasNext()) {
            String key = (String)keys.next();
            if(object.get(key) instanceof JSONObject) {
                deleteObject((JSONObject)object.get(key));
            } else if(object.get(key) instanceof JSONArray) {
                JSONArray array = (JSONArray)object.get(key);
                int len = array.length();
                for(int i = 0; i < len; i++) {
                    deleteObject(array.getJSONObject(i));
                }
            }
        }
        // delete curr doc
        deleteSingleDoc(currId);
    }

    private void deleteSingleDoc(String id) {
        DeleteRequest request = new DeleteRequest("plan", id);
        try {
            DeleteResponse updateResponse = client.delete(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            System.out.println("Error in deleteDoc from es: " + e + " for id: " + id);
        }
    }
}
