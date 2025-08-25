import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Classe principal que representa um peer no sistema de chat P2P.
 * Gerencia conexões, mensagens e coordena todas as funcionalidades do chat.
 */
public class Peer implements PeerConnection.MessageListener, PeerConnection.ConnectionListener, 
                          PeerDiscovery.PeerDiscoveryListener {
    
    private String name;
    private int port;
    private String localAddress;
    private ServerSocket serverSocket;
    private volatile boolean running;
    
    // Gerenciamento de conexões
    private Map<String, PeerConnection> connections;
    private Map<String, String> connectionNames; // address -> name mapping
    
    // Componentes do sistema
    private PeerDiscovery discovery;
    private ExecutorService connectionExecutor;
    private List<Message> messageHistory;
    
    // Listeners para notificações
    private MessageListener messageListener;
    private ConnectionStatusListener connectionStatusListener;
    
    /**
     * Interface para receber mensagens
     */
    public interface MessageListener {
        void onMessageReceived(Message message);
        void onSystemMessage(String message);
    }
    
    /**
     * Interface para notificar mudanças no status das conexões
     */
    public interface ConnectionStatusListener {
        void onPeerConnected(String peerName);
        void onPeerDisconnected(String peerName);
        void onConnectionCountChanged(int count);
    }
    
    /**
     * Construtor padrão (para uso em LAN)
     */
    public Peer(String name, int port) {
        this(name, port, null);
    }
    
    /**
     * Construtor com endereço IP personalizado (para uso via internet)
     */
    public Peer(String name, int port, String customIP) {
        this.name = name;
        this.port = port;
        
        if (customIP != null && !customIP.trim().isEmpty()) {
            // Usa IP personalizado (útil para IP público via internet)
            this.localAddress = customIP + ":" + port;
        } else {
            // Usa IP local detectado automaticamente
            this.localAddress = getLocalIPAddress() + ":" + port;
        }
        
        this.connections = new ConcurrentHashMap<>();
        this.connectionNames = new ConcurrentHashMap<>();
        this.messageHistory = Collections.synchronizedList(new ArrayList<>());
        this.connectionExecutor = Executors.newCachedThreadPool();
        this.running = false;
    }
    
    /**
     * Obtém o endereço IP local
     */
    private String getLocalIPAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
    
    /**
     * Inicia o peer
     */
    public void start() throws IOException {
        if (running) return;
        
        running = true;
        
        // Inicia servidor para aceitar conexões
        startServer();
        
        // Inicia sistema de descoberta
        startDiscovery();
        
        System.out.println("Peer " + name + " iniciado na porta " + port);
        System.out.println("Endereço local: " + localAddress);
        System.out.println("Digite 'help' para ver os comandos disponíveis");
    }
    
    /**
     * Inicia o servidor para aceitar conexões
     */
    private void startServer() throws IOException {
        serverSocket = new ServerSocket(port);
        
        // Thread para aceitar conexões
        new Thread(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleIncomingConnection(clientSocket);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                    }
                }
            }
        }).start();
    }
    
    /**
     * Inicia o sistema de descoberta
     */
    private void startDiscovery() {
        try {
            discovery = new PeerDiscovery(name, localAddress, port, this);
            discovery.start();
        } catch (Exception e) {
            System.err.println("Erro ao iniciar descoberta de peers: " + e.getMessage());
        }
    }
    
    /**
     * Trata conexão recebida
     */
    private void handleIncomingConnection(Socket socket) {
        try {
            // Primeiro recebe o nome do peer
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Object obj = in.readObject();
            
            if (obj instanceof Message) {
                Message message = (Message) obj;
                if (message.getType() == Message.MessageType.CONNECT) {
                    String peerName = message.getSenderName();
                    String peerAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
                    
                    // Cria conexão
                    PeerConnection connection = new PeerConnection(socket, peerName, this, this);
                    connections.put(peerAddress, connection);
                    connectionNames.put(peerAddress, peerName);
                    
                    // Inicia thread da conexão
                    connectionExecutor.submit(connection);
                    
                    // Notifica sobre nova conexão
                    if (connectionStatusListener != null) {
                        connectionStatusListener.onPeerConnected(peerName);
                        connectionStatusListener.onConnectionCountChanged(connections.size());
                    }
                    
                    // Envia confirmação de conexão
                    Message connectMsg = new Message(Message.MessageType.CONNECT, name, localAddress);
                    connection.sendMessage(connectMsg);
                    
                    // Adiciona mensagem ao histórico
                    addSystemMessage(peerName + " conectou-se ao chat");
                    
                    System.out.println("Nova conexão estabelecida com " + peerName + " (" + peerAddress + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar conexão recebida: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ioException) {
                // Ignora erro ao fechar
            }
        }
    }
    
    /**
     * Conecta a um peer específico
     */
    public boolean connectToPeer(String host, int port, String peerName) {
        try {
            String peerAddress = host + ":" + port;
            
            // Verifica se já está conectado
            if (connections.containsKey(peerAddress)) {
                System.out.println("Já conectado a " + peerName);
                return false;
            }
            
            // Cria conexão
            PeerConnection connection = new PeerConnection(host, port, peerName, this, this);
            connections.put(peerAddress, connection);
            connectionNames.put(peerAddress, peerName);
            
            // Inicia thread da conexão
            connectionExecutor.submit(connection);
            
            // Envia mensagem de conexão
            Message connectMsg = new Message(Message.MessageType.CONNECT, name, localAddress);
            connection.sendMessage(connectMsg);
            
            // Notifica sobre nova conexão
            if (connectionStatusListener != null) {
                connectionStatusListener.onPeerConnected(peerName);
                connectionStatusListener.onConnectionCountChanged(connections.size());
            }
            
            // Adiciona mensagem ao histórico
            addSystemMessage("Conectado a " + peerName);
            
            System.out.println("Conectado a " + peerName + " (" + peerAddress + ")");
            return true;
            
        } catch (IOException e) {
            System.err.println("Erro ao conectar a " + peerName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Desconecta de um peer específico
     */
    public boolean disconnectFromPeer(String peerName) {
        String peerAddress = null;
        for (Map.Entry<String, String> entry : connectionNames.entrySet()) {
            if (entry.getValue().equals(peerName)) {
                peerAddress = entry.getKey();
                break;
            }
        }
        
        if (peerAddress != null) {
            PeerConnection connection = connections.get(peerAddress);
            if (connection != null) {
                connection.close();
                connections.remove(peerAddress);
                connectionNames.remove(peerAddress);
                
                if (connectionStatusListener != null) {
                    connectionStatusListener.onPeerDisconnected(peerName);
                    connectionStatusListener.onConnectionCountChanged(connections.size());
                }
                
                addSystemMessage("Desconectado de " + peerName);
                System.out.println("Desconectado de " + peerName);
                return true;
            }
        }
        
        System.out.println("Peer " + peerName + " não encontrado");
        return false;
    }
    
    /**
     * Envia mensagem para todos os peers conectados
     */
    public void broadcastMessage(String content) {
        if (connections.isEmpty()) {
            System.out.println("Nenhum peer conectado para enviar mensagem");
            return;
        }
        
        Message message = new Message(name, content, localAddress);
        addMessageToHistory(message);
        
        // Envia para todas as conexões ativas
        List<String> failedConnections = new ArrayList<>();
        
        for (Map.Entry<String, PeerConnection> entry : connections.entrySet()) {
            String address = entry.getKey();
            PeerConnection connection = entry.getValue();
            
            if (!connection.sendMessage(message)) {
                failedConnections.add(address);
            }
        }
        
        // Remove conexões falhadas
        for (String address : failedConnections) {
            PeerConnection connection = connections.remove(address);
            String peerName = connectionNames.remove(address);
            if (connection != null) {
                connection.close();
            }
            if (peerName != null && connectionStatusListener != null) {
                connectionStatusListener.onPeerDisconnected(peerName);
                connectionStatusListener.onConnectionCountChanged(connections.size());
            }
        }
        
        System.out.println("Mensagem enviada para " + connections.size() + " peer(s)");
    }
    
    /**
     * Envia mensagem para um peer específico
     */
    public boolean sendMessageToPeer(String peerName, String content) {
        String peerAddress = null;
        for (Map.Entry<String, String> entry : connectionNames.entrySet()) {
            if (entry.getValue().equals(peerName)) {
                peerAddress = entry.getKey();
                break;
            }
        }
        
        if (peerAddress != null) {
            PeerConnection connection = connections.get(peerAddress);
            if (connection != null) {
                Message message = new Message(name, content, localAddress);
                addMessageToHistory(message);
                
                if (connection.sendMessage(message)) {
                    System.out.println("Mensagem enviada para " + peerName);
                    return true;
                }
            }
        }
        
        System.out.println("Peer " + peerName + " não encontrado ou conexão inativa");
        return false;
    }
    
    /**
     * Lista todos os peers conectados
     */
    public void listConnectedPeers() {
        if (connections.isEmpty()) {
            System.out.println("Nenhum peer conectado");
            return;
        }
        
        System.out.println("Peers conectados (" + connections.size() + "):");
        for (Map.Entry<String, String> entry : connectionNames.entrySet()) {
            String address = entry.getKey();
            String name = entry.getValue();
            PeerConnection connection = connections.get(address);
            String status = connection.isConnected() ? "Ativo" : "Inativo";
            System.out.println("  - " + name + " (" + address + ") - " + status);
        }
    }
    
    /**
     * Lista peers descobertos
     */
    public void listDiscoveredPeers() {
        if (discovery != null) {
            List<PeerDiscovery.DiscoveredPeer> peers = discovery.getDiscoveredPeers();
            if (peers.isEmpty()) {
                System.out.println("Nenhum peer descoberto");
                return;
            }
            
            System.out.println("Peers descobertos (" + peers.size() + "):");
            for (PeerDiscovery.DiscoveredPeer peer : peers) {
                System.out.println("  - " + peer);
            }
        }
    }
    
    /**
     * Mostra histórico de mensagens
     */
    public void showMessageHistory() {
        if (messageHistory.isEmpty()) {
            System.out.println("Nenhuma mensagem no histórico");
            return;
        }
        
        System.out.println("Histórico de mensagens (" + messageHistory.size() + "):");
        for (Message message : messageHistory) {
            System.out.println(message.getFormattedMessage());
        }
    }
    
    /**
     * Adiciona mensagem ao histórico
     */
    private void addMessageToHistory(Message message) {
        messageHistory.add(message);
        if (messageListener != null) {
            messageListener.onMessageReceived(message);
        }
    }
    
    /**
     * Adiciona mensagem de sistema ao histórico
     */
    private void addSystemMessage(String content) {
        Message systemMsg = new Message(Message.MessageType.CONNECT, "Sistema", localAddress);
        systemMsg = new Message("Sistema", content, localAddress);
        messageHistory.add(systemMsg);
        
        if (messageListener != null) {
            messageListener.onSystemMessage(content);
        }
    }
    
    /**
     * Para o peer e fecha todas as conexões
     */
    public void stop() {
        if (!running) return;
        
        running = false;
        
        // Para descoberta
        if (discovery != null) {
            discovery.stop();
        }
        
        // Fecha todas as conexões
        for (PeerConnection connection : connections.values()) {
            connection.close();
        }
        connections.clear();
        connectionNames.clear();
        
        // Fecha servidor
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar servidor: " + e.getMessage());
            }
        }
        
        // Para executor
        connectionExecutor.shutdown();
        
        System.out.println("Peer " + name + " parado");
    }
    
    // Implementação das interfaces
    
    @Override
    public void onMessageReceived(Message message) {
        addMessageToHistory(message);
        System.out.println(message.getFormattedMessage());
    }
    
    @Override
    public void onConnectionEstablished(String peerName) {
        // Já tratado no método handleIncomingConnection
    }
    
    @Override
    public void onConnectionClosed(String peerName, String reason) {
        // Remove conexão fechada
        String peerAddress = null;
        for (Map.Entry<String, String> entry : connectionNames.entrySet()) {
            if (entry.getValue().equals(peerName)) {
                peerAddress = entry.getKey();
                break;
            }
        }
        
        if (peerAddress != null) {
            connections.remove(peerAddress);
            connectionNames.remove(peerAddress);
            
            if (connectionStatusListener != null) {
                connectionStatusListener.onPeerDisconnected(peerName);
                connectionStatusListener.onConnectionCountChanged(connections.size());
            }
            
            addSystemMessage(peerName + " desconectou-se: " + reason);
            System.out.println("Conexão com " + peerName + " fechada: " + reason);
        }
    }
    
    @Override
    public void onPeerDiscovered(PeerDiscovery.DiscoveredPeer peer) {
        System.out.println("Peer descoberto: " + peer);
    }
    
    @Override
    public void onPeerLost(PeerDiscovery.DiscoveredPeer peer) {
        System.out.println("Peer perdido: " + peer);
    }
    
    // Setters para listeners
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }
    
    public void setConnectionStatusListener(ConnectionStatusListener listener) {
        this.connectionStatusListener = listener;
    }
    
    // Getters
    public String getName() { return name; }
    public int getPort() { return port; }
    public String getLocalAddress() { return localAddress; }
    public boolean isRunning() { return running; }
    public int getConnectionCount() { return connections.size(); }
}
