package com.example.cooking.handler;

import com.example.cooking.common.constant.WSMessageType;
import com.example.cooking.common.convention.wsmessage.WSMessage;
import com.example.cooking.common.web.WebSocketManager;
import com.example.cooking.service.CookingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static com.example.cooking.common.constant.WSMessageType.*;


@Component
@RequiredArgsConstructor
public class WsHandler extends TextWebSocketHandler {

    private final WebSocketManager webSocketManager;
    private static final ObjectMapper M = new ObjectMapper();

    private final CookingService cookingService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        webSocketManager.register(session.getId(), session);
        webSocketManager.send(session.getId(),
                WSMessage.buildSuccess(WSMessageType.CONNECT, session.getId(), null)
        );
        System.out.println(session.getId() + " connected");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        webSocketManager.send(session.getId(),
                WSMessage.buildSuccess(WSMessageType.DISCONNECT, session.getId(), null)
        );
        webSocketManager.unregister(session.getId());
        cookingService.unbindWsSession(session.getId());
        System.out.println(session.getId() + " disconnected");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode node = M.readTree(payload);
        String type = node.has("type") ? node.get("type").asText() : "";
        String sid = session.getId();

        System.out.println("handleTextMessage: type: " + type);
        switch (type) {
//            case BIND:
//                // 绑定 userId 或 sessionId (optional)
//                if (node.has("userId")) {
//                    String userId = node.get("userId").asText();
//                    cookingService.bindUserWs(userId, sid);
//                    send(session, createSimple("BIND_OK", "userId", userId));
//                }
//                break;
            case CREATE_SESSION:
                // 创建做菜会话，body: dishNames[]
                if (node.has("dishNames") && node.get("dishNames").isArray()) {
                    Boolean created = cookingService.createSessionWithDishNames(sid ,node.get("dishNames"));
                    if(created){
                        webSocketManager.send(sid,
                                WSMessage.buildSuccess(CREATE_SESSION, sid, null)
                        );
                    } else{
                        webSocketManager.send(sid,
                                WSMessage.buildFailure("No such dishes !")
                        );
                    }
                } else {
                    webSocketManager.send(sid,
                            WSMessage.buildFailure("No dishes provided !")
                    );
                }
                break;
            case REQUEST_NEXT:
                // poll next and reply a STEP 或 NO_STEP
                if(!cookingService.pollNextStepAndConsume(sid)){
                    System.out.println("no next step found in handler, check the ws response");
//                    webSocketManager.send(sid,
//                            WSMessage.buildSuccess(NO_NEXT_STEP, "check the prev msg !", null)
//                    );
                }
                break;
            case START_BLOCKABLE:
                // poll next and reply a STEP 或 NO_STEP
                Boolean started = cookingService.startBlockabled(sid);
                if (started) {
                    webSocketManager.send(sid,
                            WSMessage.buildSuccess(START_BLOCKABLE, sid, null)
                    );
                } else {
                    webSocketManager.send(sid,
                            WSMessage.buildFailure("This step is not blockable !")
                    );
                }
                break;
            case HEARTBEAT:
                webSocketManager.send(sid,
                        WSMessage.buildSuccess(HEARTBEAT, sid, null)
                );
                break;
            default:
                webSocketManager.send(sid,
                        WSMessage.buildFailure("Unknown message type: " + type)
                );
                break;
        }
    }
}