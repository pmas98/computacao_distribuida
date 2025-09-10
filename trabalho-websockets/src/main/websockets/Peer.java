package websockets;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Peer {
    private final int port;
    private final String username;
    private ServerSocket serverSocket;
    private boolean running = false;
    private final Set<PeerConnection> connections;
    private final ExecutorService threadPool;
    private final List<String> messageHistory;
    private final Set<UUID> processedMessages;
    private PeerDiscovery peerDiscovery;

    public Peer(int port, String username) {
        this.port = port;
        this.username = username;
        this.connections = Collections.synchronizedSet(new HashSet<>());
        this.threadPool = Executors.newCachedThreadPool();
        this.messageHistory = new CopyOnWriteArrayList<>();
        this.processedMessages = Collections.synchronizedSet(new HashSet<>());
    }

    public boolean start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            
            threadPool.submit(this::acceptConnections);
            
            try {
                peerDiscovery = new PeerDiscovery(this);
                threadPool.submit(peerDiscovery);
            } catch (IOException e) {
                System.err.println("Erro ao iniciar a descoberta de peers: " + e.getMessage());
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor na porta " + port + ": " + e.getMessage());
            return false;
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                PeerConnection connection = new PeerConnection(clientSocket, this);
                connections.add(connection);
                threadPool.submit(connection);
                
                System.out.println("\nNova conexão aceita de: " + 
                    clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                System.out.print("> ");
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                }
            }
        }
    }

    public boolean connectToPeer(String host, int port) {
        if (port == 0 && peerDiscovery != null) {
            Map<String, String> discoveredPeers = peerDiscovery.getDiscoveredPeers();
            String address = discoveredPeers.get(host);
            if (address != null) {
                String[] parts = address.split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            } else {
                System.err.println("Peer com nome de usuário '" + host + "' não encontrado.");
                return false;
            }
        }

        try {
            Socket socket = new Socket(host, port);
            PeerConnection connection = new PeerConnection(socket, this);
            connections.add(connection);
            threadPool.submit(connection);
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao conectar com " + host + ":" + port + " - " + e.getMessage());
            return false;
        }
    }

    public Map<String, String> getDiscoveredPeers() {
        if (peerDiscovery != null) {
            return peerDiscovery.getDiscoveredPeers();
        }
        return Collections.emptyMap();
    }
    
    public void broadcastMessage(String content) {
        if (connections.isEmpty()) {
            System.out.println("Nenhuma conexão ativa para enviar mensagem");
            return;
        }

        Message message = new Message(username, content, Message.MessageType.TEXT);
        processedMessages.add(message.getId()); 

        System.out.println("Enviando: " + message);
        messageHistory.add("Eu: " + content);
        
        synchronized (connections) {
            for (PeerConnection connection : connections) {
                connection.sendMessage(message);
            }
        }
    }
    
    public void relayMessage(Message message) {
        if (processedMessages.contains(message.getId())) {
            return;
        }
    
        processedMessages.add(message.getId());

        if (message.getType() == Message.MessageType.DISCONNECT) {
            messageHistory.add("Usuário " + message.getSenderUsername() + " desconectado.");
            System.out.println("\nUsuário " + message.getSenderUsername() + " desconectado.");

            PeerConnection connectionToRemove = null;
            synchronized (connections) {
                for (PeerConnection connection : connections) {
                    if (connection.getRemoteUsername().equals(message.getSenderUsername())) {
                        connectionToRemove = connection;
                        break;
                    }
                }
                if (connectionToRemove != null) {
                    connectionToRemove.close();
                    connections.remove(connectionToRemove);
                }
            }
        } else {
            messageHistory.add(message.toString());
            System.out.println("\n" + message);
        }

        System.out.print("> ");
    
        synchronized (connections) {
            for (PeerConnection connection : connections) {
                if (!connection.getRemoteUsername().equals(message.getSenderUsername())) {
                    connection.sendMessage(message);
                }
            }
        }
    }

    public void listConnections() {
        if (connections.isEmpty()) {
            System.out.println("Nenhuma conexão ativa");
        } else {
            System.out.println("\n=== Conexões Ativas ===");
            int i = 1;
            synchronized (connections) {
                for (PeerConnection connection : connections) {
                    System.out.println(i + ". " + connection.getRemoteUsername() + "@" + connection.getRemoteAddress() + 
                        " (status: " + (connection.isConnected() ? "conectado" : "desconectado") + ")");
                    i++;
                }
            }
            System.out.println("=======================\n");
        }
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public void printMessageHistory() {
        System.out.println("\n=== Histórico de Mensagens ===");
        if (messageHistory.isEmpty()) {
            System.out.println("Nenhuma mensagem no histórico.");
        } else {
            for (String msg : messageHistory) {
                System.out.println(msg);
            }
        }
        System.out.println("============================\n");
    }

    public void stop() {
        running = false;

        if (peerDiscovery != null) {
            peerDiscovery.stop();
        }
        
        Message disconnectMessage = new Message(username, "desconectando", Message.MessageType.DISCONNECT);
        synchronized (connections) {
            for (PeerConnection connection : connections) {
                connection.sendMessage(disconnectMessage);
                connection.close();
            }
            connections.clear();
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar servidor: " + e.getMessage());
            }
        }

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }

    private class PeerConnection implements Runnable {
        private Socket socket;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private boolean connected = true;
        private String remoteUsername;
        private Peer owner;

        public PeerConnection(Socket socket, Peer owner) {
            this.socket = socket;
            this.owner = owner;
            
            try {
                this.oos = new ObjectOutputStream(socket.getOutputStream());
                this.ois = new ObjectInputStream(socket.getInputStream());
                
                // Troca de nomes de usuário
                oos.writeObject(owner.getUsername());
                this.remoteUsername = (String) ois.readObject();

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro ao configurar streams: " + e.getMessage());
                connected = false;
            }
        }

        @Override
        public void run() {
            try {
                while (connected) {
                    Message message = (Message) ois.readObject();
                    if (message.getType() == Message.MessageType.DISCONNECT) {
                        System.out.println("\n" + message.getSenderUsername() + " desconectado.");
                        System.out.print("> ");
                        
                        // Fecha a conexão do lado do cliente
                        close();
                        owner.connections.remove(this);
                    } else {
                        owner.relayMessage(message);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    System.out.println("\nConexão perdida com " + getRemoteUsername() + "@" + getRemoteAddress());
                    System.out.print("> ");
                }
            } finally {
                close();
                connections.remove(this);
            }
        }

        public void sendMessage(Message message) {
            try {
                if (connected && oos != null) {
                    oos.writeObject(message);
                }
            } catch (IOException e) {
                System.err.println("Erro ao enviar mensagem para " + remoteUsername + ": " + e.getMessage());
            }
        }

        public String getRemoteAddress() {
            if (socket != null && !socket.isClosed()) {
                return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            }
            return "desconhecido";
        }

        public String getRemoteUsername() {
            return remoteUsername != null ? remoteUsername : "desconhecido";
        }

        public boolean isConnected() {
            return connected && socket != null && !socket.isClosed();
        }

        public void close() {
            connected = false;
            
            try {
                if (ois != null) ois.close();
                if (oos != null) oos.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
}
