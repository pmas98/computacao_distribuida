package websockets;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Peer {
    private final int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    private final Set<PeerConnection> connections;
    private final ExecutorService threadPool;

    public Peer(int port) {
        this.port = port;
        this.connections = Collections.synchronizedSet(new HashSet<>());
        this.threadPool = Executors.newCachedThreadPool();
    }

    public boolean start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            
            threadPool.submit(this::acceptConnections);
            
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
                PeerConnection connection = new PeerConnection(clientSocket, false);
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
        try {
            Socket socket = new Socket(host, port);
            PeerConnection connection = new PeerConnection(socket, true);
            connections.add(connection);
            threadPool.submit(connection);
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao conectar com " + host + ":" + port + " - " + e.getMessage());
            return false;
        }
    }

    public void broadcastMessage(String message) {
        if (connections.isEmpty()) {
            System.out.println("Nenhuma conexão ativa para enviar mensagem");
            return;
        }

        String formattedMessage = "[" + new Date() + "] " + message;
        System.out.println("Enviando: " + formattedMessage);
        
        synchronized (connections) {
            for (PeerConnection connection : connections) {
                connection.sendMessage(formattedMessage);
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
                    System.out.println(i + ". " + connection.getRemoteAddress() + 
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

    public void stop() {
        running = false;
        
        synchronized (connections) {
            for (PeerConnection connection : connections) {
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
        private BufferedReader reader;
        private PrintWriter writer;
        private boolean connected = true;

        public PeerConnection(Socket socket, boolean isOutgoing) {
            this.socket = socket;
            
            try {
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writer = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("Erro ao configurar streams: " + e.getMessage());
                connected = false;
            }
        }

        @Override
        public void run() {
            try {
                String message;
                while (connected && (message = reader.readLine()) != null) {
                    System.out.println("\nMensagem recebida de " + getRemoteAddress() + ": " + message);
                    System.out.print("> ");
                }
            } catch (IOException e) {
                if (connected) {
                    System.out.println("\nConexão perdida com " + getRemoteAddress());
                    System.out.print("> ");
                }
            } finally {
                close();
                connections.remove(this);
            }
        }

        public void sendMessage(String message) {
            if (connected && writer != null) {
                writer.println(message);
            }
        }

        public String getRemoteAddress() {
            if (socket != null && !socket.isClosed()) {
                return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            }
            return "desconhecido";
        }

        public boolean isConnected() {
            return connected && socket != null && !socket.isClosed();
        }

        public void close() {
            connected = false;
            
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
}
