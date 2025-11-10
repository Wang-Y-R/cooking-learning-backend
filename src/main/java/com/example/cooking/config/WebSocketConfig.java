package com.example.cooking.config;


import com.example.cooking.handler.WsHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WsHandler wsHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        /*
         "/ws"：客户端连接的路径（URL），浏览器或小程序连接时就要用这个路径

        .setAllowedOrigins("*"):
        WebSocket 也有跨域限制（Origin 验证），默认情况下不同域名会被拒绝
        * 表示允许 所有域名 访问
        如果你上线了正式域名，可以改成：setAllowedOrigins("https://www.mydomain.com")
         */
        registry.addHandler(wsHandler, "/ws").setAllowedOrigins("*");
    }
}
