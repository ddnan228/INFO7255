package com.example.restservice.jsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapService {

    // validate the id and id in json for PUT and Patch
    public static boolean validateObjectId(String id, String body) {
        try {
            JSONObject jObject = new JSONObject(body);
            String key = jObject.get("objectId").toString();
            return key.equals(id);
        } catch (Exception e) {
            return false;
        }
    }

    // patch the json string
    // if internal object_id change, add a new internal object to array
    // if content change, do a modification on the orig json
    // return the modified json string for storage
    public static String patchJson(String orig, String curr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode originalParent = JsonLoader.fromString(orig);
        JsonNode currParent = JsonLoader.fromString(curr);

        JsonNode patched = patchNode(originalParent, currParent);
        String patchedJson = objectMapper.writeValueAsString(patched);
        return patchedJson;
    }

    private static JsonNode patchNode(JsonNode orig, JsonNode curr) {
        if(orig.isValueNode()){
            // System.out.println("patch for values");
            return curr;
        }
        if(curr == null || curr.isEmpty()) {
            return orig;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode resNode = objectMapper.createObjectNode();

        if(orig.isObject()) {
            if(!orig.get("objectId").equals(curr.get("objectId"))) {
                ArrayNode arrayNode = objectMapper.createArrayNode();
                arrayNode.addAll(Arrays.asList(orig, curr));
                return arrayNode;
            } else {
                //iterator fields and compare
                Iterator<String> fieldNames = orig.fieldNames();
                while(fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    JsonNode fieldValue = orig.get(fieldName);
                    JsonNode patch = patchNode(fieldValue, curr.get(fieldName));
                    // System.out.println("patch for values");
                    resNode.set(fieldName, patch);
                }
                return resNode;
            }
        } else if(orig.isArray()) {
            if(!curr.isArray()) {
                // iterate orig to find whether there is same object id
                // if has do recursive patch, if not add curr to orig array
                boolean sameID = false;
                ArrayNode arrayNode = (ArrayNode) orig;
                for(int i = 0; i < arrayNode.size(); i++) {
                    JsonNode arrayElement = arrayNode.get(i);
                    if(arrayElement.get("objectId").equals(curr.get("objectId"))) {
                        sameID = true;
                        JsonNode patch = patchNode(arrayElement, curr);
                        arrayNode.set(i, patch);
                        break;
                    }
                }
                if(!sameID){
                    arrayNode.add(curr);
                }
                return arrayNode;
            } else {
                // two loop
                ArrayNode arrayNode = (ArrayNode) orig;
                ArrayNode currArrayNode = (ArrayNode) curr;
                for(int i = 0; i < currArrayNode.size(); i++) {
                    JsonNode currArrayElement = currArrayNode.get(i);
                    boolean sameID = false;
                    for(int j = 0; j < arrayNode.size(); j++) {
                        JsonNode arrayElement = arrayNode.get(j);
                        if(arrayElement.get("objectId").equals(currArrayElement.get("objectId"))) {
                            sameID = true;
                            JsonNode patch = patchNode(arrayElement, currArrayElement);
                            arrayNode.set(j, patch);
                            break;
                        }
                    }
                    if(!sameID){
                        arrayNode.add(currArrayElement);
                    }
                }
                return arrayNode;
            }
        } else {
            System.out.println("why not here");
            return curr;
        }
    }
}
