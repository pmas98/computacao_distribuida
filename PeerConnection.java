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
        try {
            // Valida parâmetros
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("Host não pode ser nulo ou vazio");
            }
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Porta inválida: " + port);
            }
            if (remotePeerName == null || remotePeerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Nome do peer não pode ser nulo ou vazio");
            }
            
            System.out.println("Tentando conectar a " + remotePeerName + " em " + host + ":" + port);
            
            // Cria socket com timeout
            this.socket = new Socket();
            this.socket.connect(new java.net.InetSocketAddress(host, port), 10000); // 10 segundos timeout
            
            this.remotePeerName = remotePeerName;
            this.remoteAddress = host + ":" + port;
            this.messageListener = messageListener;
            this.connectionListener = connectionListener;
            this.running = new AtomicBoolean(true);
            
            System.out.println("Socket criado com sucesso para " + remotePeerName);
            
            setupStreams();
            
            // Só notifica se os streams foram configurados com sucesso
            if (out != null && in != null) {
                connectionListener.onConnectionEstablished(remotePeerName);
                System.out.println("Conexão estabelecida com " + remotePeerName + " em " + remoteAddress);
            } else {
                throw new IOException("Falha ao configurar streams de comunicação");
            }
            
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            System.err.println("Erro ao criar conexão com " + remotePeerName + ": " + errorMsg);
            if (e.getCause() != null) {
                System.err.println("Causa: " + e.getCause().getMessage());
            }
            
            // Limpa recursos em caso de erro
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException io) {}
            }
            
            throw new IOException("Falha ao conectar a " + remotePeerName + " em " + host + ":" + port + ": " + errorMsg, e);
        }
    }
    
    /**
     * Construtor para conexões passivas (servidor)
     */
    public PeerConnection(Socket socket, String remotePeerName, 
                         MessageListener messageListener, ConnectionListener connectionListener) {
        try {
            // Valida parâmetros
            if (socket == null) {
                throw new IllegalArgumentException("Socket não pode ser nulo");
            }
            if (remotePeerName == null || remotePeerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Nome do peer não pode ser nulo ou vazio");
            }
            
            this.socket = socket;
            this.remotePeerName = remotePeerName;
            this.remoteAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            this.messageListener = messageListener;
            this.connectionListener = connectionListener;
            this.running = new AtomicBoolean(true);
            
            System.out.println("Processando conexão recebida de " + remotePeerName + " em " + remoteAddress);
            
            setupStreams();
            
            // Só notifica se os streams foram configurados com sucesso
            if (out != null && in != null) {
                connectionListener.onConnectionEstablished(remotePeerName);
                System.out.println("Conexão passiva estabelecida com " + remotePeerName + " em " + remoteAddress);
            } else {
                throw new IOException("Falha ao configurar streams de comunicação para conexão passiva");
            }
            
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            System.err.println("Erro ao processar conexão passiva de " + remotePeerName + ": " + errorMsg);
            if (e.getCause() != null) {
                System.err.println("Causa: " + e.getCause().getMessage());
            }
            
            // Limpa recursos em caso de erro
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException io) {}
            }
            
            // Não notifica o listener em caso de erro
            this.running.set(false);
        }
    }
    
    /**
     * Configura os streams de entrada e saída
     */
    private void setupStreams() {
        try {
            // Verifica se o socket é válido
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                throw new IOException("Socket inválido ou fechado");
            }
            
            // Configura timeout para evitar travamentos
            socket.setSoTimeout(30000); // 30 segundos
            
            // Cria streams na ordem correta (ObjectOutputStream primeiro)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // Força envio do header
            
            in = new ObjectInputStream(socket.getInputStream());
            
            System.out.println("Streams configurados com sucesso para " + remotePeerName);
            
        } catch (IOException e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            System.err.println("Erro ao configurar streams para " + remotePeerName + ": " + errorMsg);
            if (e.getCause() != null) {
                System.err.println("Causa: " + e.getCause().getMessage());
            }
            close();
        } catch (Exception e) {
            System.err.println("Erro inesperado ao configurar streams para " + remotePeerName + ": " + e.getMessage());
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
