package com.example.cooking.common.web;

import com.example.cooking.service.CookingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WsHandler extends TextWebSocketHandler {

    // 全局 WebSocket 连接表： wsSessionId -> WebSocketSession
    // 用于后端主动给前端发消息时找到对应的session
    public static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final ObjectMapper M = new ObjectMapper();

    private final CookingService cookingService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        // 可在这里发送 welcome
        System.out.println(session.getId() + " connected");
        send(session, M.createObjectNode().put("type","CONNECTED").put("wsSessionId", session.getId()).toString());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        // 解绑 cookingService 中可能绑定的 wsSessionId

        System.out.println(session.getId() + " disconnected");
        cookingService.unbindWsSession(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode node = M.readTree(payload);
        String type = node.has("type") ? node.get("type").asText() : "";
        String sid = session.getId();

        System.out.println("handleTextMessage: type: " + type);
        switch (type) {
//            case "BIND":
//                // 绑定 userId 或 sessionId (optional)
//                if (node.has("userId")) {
//                    String userId = node.get("userId").asText();
//                    cookingService.bindUserWs(userId, session.getId());
//                    send(session, createSimple("BIND_OK", "userId", userId));
//                }
//                break;
            case "CREATE_SESSION":
                // 创建做菜会话，body: dishNames[]
                if (node.has("dishNames") && node.get("dishNames").isArray()) {
                    Boolean created = cookingService.createSessionWithDishNames(session.getId() ,node.get("dishNames"));
                    if(created){
                        send(session, createSimpleMessage("CREATE_SESSION", "success"));
                    } else{
                        send(session, createError("dishes not exist"));
                    }
                } else {
                    send(session, createError("CREATE_SESSION missing dishNames"));
                }
                break;
            case "REQUEST_NEXT":
                // poll next and reply a STEP 或 NO_STEP
                String stepJson = cookingService.pollNextStepAndConsume(sid);
                if (stepJson == null) {
                    send(session, createSimple("NO_STEP", "sessionId", sid));
                } else {
                    send(session, stepJson);
                }
                break;
            case "START_BLOCKABLE":
                // poll next and reply a STEP 或 NO_STEP
                Boolean started = cookingService.startBlockabled(sid);
                if (started) {
                    send(session, createSimpleMessage("START_BLOCKABLE", "success"));
                } else {
                    send(session, createError("Not Blockable!"));
                }
                break;
            case "HEARTBEAT":
                send(session, createSimple("HEARTBEAT_ACK", "ts", String.valueOf(System.currentTimeMillis())));
                break;
            default:
                send(session, createError("Unknown message type: " + type));
        }
    }

    // 工具：发送给指定 session
    public static void sendToWsSession(String sid, String text) {
        WebSocketSession s = sessions.get(sid);
        if (s != null && s.isOpen()) {
            try {
                s.sendMessage(new TextMessage(text));
            } catch (Exception e) {
                // 忽略或记录
            }
        }
    }

    private static void send(WebSocketSession s, String payload) {
        try {
            if (s != null && s.isOpen()) s.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            // 忽略或记录
            System.err.println("ws send error in WsHandler.send()");
        }
    }

    // 构造函数（JSON 字符串）
    private static String createSimple(String type, String key, String val) {
        try {
            return M.createObjectNode().put("type",type).put(key,val).toString();
        } catch (Exception e) { return "{\"type\":\"ERROR\"}"; }
    }

    private static String createSimpleMessage(String type, String msg) {
        try {
            return M.createObjectNode().put("type",type).put("message",msg).toString();
        } catch (Exception e) { return "{\"type\":\"ERROR\"}"; }
    }

    private static String createError(String msg) {
        try {
            return M.createObjectNode().put("type","ERROR").put("message",msg).toString();
        } catch (Exception e) { return "{\"type\":\"ERROR\"}"; }
    }
}