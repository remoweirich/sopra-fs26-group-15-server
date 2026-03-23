package websocket;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;

public class Message {
    private MessageType type;

    private Object payload;

    public Message(MessageType type, T payload) {
        this.type = type;
        this.payload = payload;
    }
}
