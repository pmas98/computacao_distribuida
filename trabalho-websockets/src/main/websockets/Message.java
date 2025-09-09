package websockets;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        TEXT,
        DISCONNECT
    }

    private final UUID id;
    private final String senderUsername;
    private final String content;
    private final long timestamp;
    private final MessageType type;

    public Message(String senderUsername, String content, MessageType type) {
        this.id = UUID.randomUUID();
        this.senderUsername = senderUsername;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "[" + senderUsername + "]: " + content;
    }
}
