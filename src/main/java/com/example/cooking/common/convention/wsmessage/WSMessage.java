package com.example.cooking.common.convention.wsmessage;

import com.example.cooking.common.constant.WSMessageType;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class WSMessage<T> implements Serializable {


    private String type;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public static <T> WSMessage<T> buildSuccess(String type, String msg ,T data) {
        return new WSMessage<T>(type, msg, data);
    }

    public static <T> WSMessage<T> buildFailure(String msg) {
        return new WSMessage<T>(WSMessageType.ERROR, msg, null);
    }


}
