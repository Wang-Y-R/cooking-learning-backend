package com.example.cooking.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface CookingService {
    void unbindWsSession(String sid);


//    void bindUserWs(String uid, String sid);

    Boolean createSessionWithDishNames(String sid, JsonNode dishesName);


    String pollNextStepAndConsume(String sid);

    Boolean startBlockabled(String sid);

}
