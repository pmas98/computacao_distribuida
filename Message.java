import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe que representa uma mensagem trocada entre peers no sistema de chat P2P.
 * Implementa Serializable para permitir transmissão via sockets.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        CHAT,           // Mensagem de chat normal
        CONNECT,        // Solicitação de conexão
        DISCONNECT,     // Notificação de desconexão
        PING,          // Ping para verificar conectividade
        PONG           // Resposta ao ping
    }
    
    private String senderName;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private String senderAddress;
    
    /**
     * Construtor para mensagens de chat normais
     */
    public Message(String senderName, String content, String senderAddress) {
        this.senderName = senderName;
        this.content = content;
        this.type = MessageType.CHAT;
        this.timestamp = LocalDateTime.now();
        this.senderAddress = senderAddress;
    }
    
    /**
     * Construtor para mensagens de sistema
     */
    public Message(MessageType type, String senderName, String senderAddress) {
        this.type = type;
        this.senderName = senderName;
        this.senderAddress = senderAddress;
        this.timestamp = LocalDateTime.now();
        this.content = "";
    }
    
    // Getters
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public MessageType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSenderAddress() { return senderAddress; }
    
    /**
     * Retorna a mensagem formatada para exibição
     */
    public String getFormattedMessage() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timeStr = timestamp.format(formatter);
        
        switch (type) {
            case CHAT:
                return String.format("[%s] %s: %s", timeStr, senderName, content);
            case CONNECT:
                return String.format("[%s] %s conectou-se ao chat", timeStr, senderName);
            case DISCONNECT:
                return String.format("[%s] %s desconectou-se do chat", timeStr, senderName);
            case PING:
                return String.format("[%s] Ping de %s", timeStr, senderName);
            case PONG:
                return String.format("[%s] Pong de %s", timeStr, senderName);
            default:
                return String.format("[%s] %s", timeStr, content);
        }
    }
    
    @Override
    public String toString() {
        return getFormattedMessage();
    }
}
