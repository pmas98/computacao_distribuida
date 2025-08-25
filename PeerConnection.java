import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Classe que gerencia uma conexão individual com outro peer.
 * Responsável por enviar e receber mensagens através de uma conexão TCP.
 */
public class PeerConnection implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String remotePeerName;
    private String remoteAddress;
    private MessageListener messageListener;
    private ConnectionListener connectionListener;
    private AtomicBoolean running;
    
    /**
     * Interface para receber mensagens de peers conectados
     */
    public interface MessageListener {
        void onMessageReceived(Message message);
    }
    
    /**
     * Interface para notificar mudanças no status da conexão
     */
    public interface ConnectionListener {
        void onConnectionClosed(String peerName, String reason);
        void onConnectionEstablished(String peerName);
    }
    
    /**
     * Construtor para conexões ativas (cliente)
     */
    public PeerConnection(String host, int port, String remotePeerName, 
                         MessageListener messageListener, ConnectionListener connectionListener) 
                         throws IOException {
        this.socket = new Socket(host, port);
        this.remotePeerName = remotePeerName;
        this.remoteAddress = host + ":" + port;
        this.messageListener = messageListener;
        this.connectionListener = connectionListener;
        this.running = new AtomicBoolean(true);
        
        setupStreams();
        connectionListener.onConnectionEstablished(remotePeerName);
    }
    
    /**
     * Construtor para conexões passivas (servidor)
     */
    public PeerConnection(Socket socket, String remotePeerName, 
                         MessageListener messageListener, ConnectionListener connectionListener) {
        this.socket = socket;
        this.remotePeerName = remotePeerName;
        this.remoteAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        this.messageListener = messageListener;
        this.connectionListener = connectionListener;
        this.running = new AtomicBoolean(true);
        
        setupStreams();
        connectionListener.onConnectionEstablished(remotePeerName);
    }
    
    /**
     * Configura os streams de entrada e saída
     */
    private void setupStreams() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Erro ao configurar streams: " + e.getMessage());
            close();
        }
    }
    
    /**
     * Envia uma mensagem para o peer conectado
     */
    public synchronized boolean sendMessage(Message message) {
        if (!running.get() || out == null) {
            return false;
        }
        
        try {
            out.writeObject(message);
            out.flush();
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem para " + remotePeerName + ": " + e.getMessage());
            close();
            return false;
        }
    }
    
    /**
     * Loop principal para receber mensagens
     */
    @Override
    public void run() {
        try {
            while (running.get() && !socket.isClosed()) {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    Message message = (Message) obj;
                    messageListener.onMessageReceived(message);
                }
            }
        } catch (EOFException e) {
            // Conexão fechada pelo peer remoto
            System.out.println("Conexão com " + remotePeerName + " foi fechada pelo peer remoto");
        } catch (IOException | ClassNotFoundException e) {
            if (running.get()) {
                System.err.println("Erro na conexão com " + remotePeerName + ": " + e.getMessage());
            }
        } finally {
            close();
        }
    }
    
    /**
     * Fecha a conexão de forma segura
     */
    public void close() {
        if (running.compareAndSet(true, false)) {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar conexão: " + e.getMessage());
            } finally {
                connectionListener.onConnectionClosed(remotePeerName, "Conexão fechada");
            }
        }
    }
    
    /**
     * Verifica se a conexão está ativa
     */
    public boolean isConnected() {
        return running.get() && socket != null && !socket.isClosed();
    }
    
    /**
     * Envia um ping para verificar conectividade
     */
    public boolean sendPing() {
        Message pingMessage = new Message(Message.MessageType.PING, "System", remoteAddress);
        return sendMessage(pingMessage);
    }
    
    // Getters
    public String getRemotePeerName() { return remotePeerName; }
    public String getRemoteAddress() { return remoteAddress; }
    public Socket getSocket() { return socket; }
}
