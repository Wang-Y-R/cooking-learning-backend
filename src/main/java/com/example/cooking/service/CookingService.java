package com.example.cooking.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface CookingService {
    void unbindWsSession(String sid);


//    void bindUserWs(String uid, String sid);

    Boolean createSessionWithDishNames(String sid, JsonNode dishesName);

    default boolean createSession(String sid, JsonNode dishesName) {
        return createSessionWithDishNames(sid, dishesName);
    }


    Boolean pollNextStepAndConsume(String sid);

    Boolean startBlockabled(String sid);

    java.util.Optional<com.example.cooking.dao.entity.CookingRuntime> getRuntime(String sid);

}
