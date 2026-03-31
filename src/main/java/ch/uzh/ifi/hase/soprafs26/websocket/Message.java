package ch.uzh.ifi.hase.soprafs26.websocket;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import org.springframework.messaging.handler.annotation.Payload;

public class Message {
    private MessageType type;

    private Object payload;

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public void setType(MessageType type) { this.type = type; }

    public void setPayload(Object payload) { this.payload = payload; }

    public MessageType getType() { return type; }

    public Object getPayload() { return payload; }


}
