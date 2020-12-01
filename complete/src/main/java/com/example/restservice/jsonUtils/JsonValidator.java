package com.example.restservice.jsonUtils;

import com.example.restservice.storeService.JedisClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import redis.clients.jedis.Jedis;

import java.io.IOException;

public class JsonValidator {
    public static String SCHEMA_IDENTIFIER = "plan_schema_json";
    public static String SCHEMA_ELEMENT = "$schema";

    public static boolean validateJson(String data) throws IOException, ProcessingException {
        Jedis jedis = JedisClient.getJedisClient();
        jedis.connect();
        String planSchema = jedis.get("planKey");
        return isJsonValidateWithSchema(data, planSchema);
    }

    private static boolean isJsonValidateWithSchema(String data, String schema) throws IOException, ProcessingException {
        final JsonSchema schemaNode = getSchemaNode(schema);
        final JsonNode dataNode = getJsonNode(data);
        ProcessingReport report = schemaNode.validate(dataNode);
        return report.isSuccess();
    }

    public static JsonSchema getSchemaNode(String schema) throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNode(schema);
        final JsonNode schemaIdentifier = schemaNode.get(SCHEMA_ELEMENT);
        if (schemaIdentifier == null) {
            ((ObjectNode) schemaNode).put(SCHEMA_ELEMENT, SCHEMA_IDENTIFIER);
        }
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        return factory.getJsonSchema(schemaNode);
    }

    public static JsonNode getJsonNode(String json) throws IOException {
        return JsonLoader.fromString(json);
    }

}
