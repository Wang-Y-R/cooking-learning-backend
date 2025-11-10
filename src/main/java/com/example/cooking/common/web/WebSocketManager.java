package com.example.cooking.common.web;

import com.example.cooking.common.convention.wsmessage.WSMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketManager {

    // 全局 WebSocket 连接表： wsSessionId -> WebSocketSession
    // 用于后端主动给前端发消息时找到对应的session
    public static final Map<String, WebSocketSession> wsSessions = new ConcurrentHashMap<>();

    private static final ObjectMapper M = new ObjectMapper();


    public void register(String wsId, WebSocketSession session) {
        wsSessions.put(wsId, session);
    }

    public void unregister(String wsId) {
        wsSessions.remove(wsId);
//        // 如果某个 biz 指向这个 wsId，应将其解绑（可选，具体业务决定）
//        bizToWs.forEach((biz, mappedWs) -> {
//            if (wsId.equals(mappedWs)) {
//                bizToWs.remove(biz, wsId);
//            }
//        });
    }

//    public void bindBizToWs(String bizId, String wsId) {
//        if (bizId == null || wsId == null) return;
//        bizToWs.put(bizId, wsId);
//    }

//    public void unbindBiz(String bizId) {
//        bizToWs.remove(bizId);
//    }

    public WebSocketSession getSessionByWsId(String wsId) {
        return wsSessions.get(wsId);
    }

//    public String getWsIdByBiz(String bizId) {
//        return bizToWs.get(bizId);
//    }

    public <T> boolean send(String wsId, WSMessage<T> wsMessage) {
        WebSocketSession s = wsSessions.get(wsId);
        if (s != null && s.isOpen()) {
            String payload = M.valueToTree(wsMessage).toString();
            try {
                s.sendMessage(new TextMessage(payload));
                return true;
            } catch (Exception e) {
                // 记录日志，可能需要重试或清理
            }
        }
        return false;
    }

//    public boolean sendToBiz(String bizId, String payload) {
//        String wsId = getWsIdByBiz(bizId);
//        if (wsId == null) return false;
//        return sendToWs(wsId, payload);
//    }

}
