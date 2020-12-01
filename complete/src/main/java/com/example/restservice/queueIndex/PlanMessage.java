package com.example.restservice.queueIndex;

public class PlanMessage {
    public String id;
    public Boolean isDelete;
    public String jsonBody;

    public PlanMessage(String id, Boolean isDelete, String jsonBody) {
        this.id = id;
        this.isDelete = isDelete;
        this.jsonBody = jsonBody;
    }
}
