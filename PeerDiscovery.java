import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Classe responsável pela descoberta automática de peers na rede.
 * Implementa um sistema de broadcast UDP para encontrar outros peers ativos.
 */
public class PeerDiscovery implements Runnable {
    private static final int DISCOVERY_PORT = 8888;
    private static final String DISCOVERY_MESSAGE = "P2P_CHAT_DISCOVERY";
    private static final int BROADCAST_INTERVAL = 5000; // 5 segundos
    
    private DatagramSocket socket;
    private InetAddress broadcastAddress;
    private String localPeerName;
    private String localAddress;
    private volatile boolean running;
    private Map<String, DiscoveredPeer> discoveredPeers;
    private PeerDiscoveryListener listener;
    private ScheduledExecutorService scheduler;
    
    /**
     * Classe que representa um peer descoberto
     */
    public static class DiscoveredPeer {
        private String name;
        private String address;
        private int port;
        private long lastSeen;
        
        public DiscoveredPeer(String name, String address, int port) {
            this.name = name;
            this.address = address;
            this.port = port;
            this.lastSeen = System.currentTimeMillis();
        }
        
        public void updateLastSeen() {
            this.lastSeen = System.currentTimeMillis();
        }
        
        // Getters
        public String getName() { return name; }
        public String getAddress() { return address; }
        public int getPort() { return port; }
        public long getLastSeen() { return lastSeen; }
        
        @Override
        public String toString() {
            return String.format("%s (%s:%d)", name, address, port);
        }
    }
    
    /**
     * Interface para notificar sobre peers descobertos
     */
    public interface PeerDiscoveryListener {
        void onPeerDiscovered(DiscoveredPeer peer);
        void onPeerLost(DiscoveredPeer peer);
    }
    
    /**
     * Construtor
     */
    public PeerDiscovery(String localPeerName, String localAddress, int localPort, 
                        PeerDiscoveryListener listener) throws SocketException, UnknownHostException {
        this.localPeerName = localPeerName;
        this.localAddress = localAddress;
        this.listener = listener;
        this.discoveredPeers = new ConcurrentHashMap<>();
        this.running = false;
        
        // Configura socket UDP para descoberta
        socket = new DatagramSocket();
        socket.setBroadcast(true);
        
        // Encontra endereço de broadcast
        try {
            broadcastAddress = InetAddress.getByName("255.255.255.255");
        } catch (UnknownHostException e) {
            // Fallback para endereço de broadcast padrão
            broadcastAddress = InetAddress.getByName("255.255.255.255");
        }
        
        // Configura scheduler para limpeza periódica
        scheduler = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * Inicia o processo de descoberta
     */
    public void start() {
        if (!running) {
            running = true;
            
            // Inicia thread principal
            new Thread(this).start();
            
            // Inicia broadcast periódico
            scheduler.scheduleAtFixedRate(this::sendBroadcast, 0, BROADCAST_INTERVAL, TimeUnit.MILLISECONDS);
            
            // Inicia limpeza periódica de peers inativos
            scheduler.scheduleAtFixedRate(this::cleanupInactivePeers, 10000, 10000, TimeUnit.MILLISECONDS);
            
            System.out.println("Descoberta de peers iniciada na porta " + DISCOVERY_PORT);
        }
    }
    
    /**
     * Para o processo de descoberta
     */
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
    
    /**
     * Envia broadcast de descoberta
     */
    private void sendBroadcast() {
        if (!running) return;
        
        try {
            String message = DISCOVERY_MESSAGE + "|" + localPeerName + "|" + localAddress;
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
            socket.send(packet);
        } catch (IOException e) {
            if (running) {
                System.err.println("Erro ao enviar broadcast: " + e.getMessage());
            }
        }
    }
    
    /**
     * Loop principal para receber mensagens de descoberta
     */
    @Override
    public void run() {
        try {
            while (running) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                try {
                    socket.receive(packet);
                    processDiscoveryMessage(packet);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Erro ao receber mensagem de descoberta: " + e.getMessage());
                    }
                }
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
    
    /**
     * Processa mensagem de descoberta recebida
     */
    private void processDiscoveryMessage(DatagramPacket packet) {
        try {
            String message = new String(packet.getData(), 0, packet.getLength());
            String[] parts = message.split("\\|");
            
            if (parts.length == 3 && parts[0].equals(DISCOVERY_MESSAGE)) {
                String peerName = parts[1];
                String peerAddress = parts[2];
                
                // Ignora mensagens do próprio peer
                if (peerName.equals(localPeerName) && peerAddress.equals(localAddress)) {
                    return;
                }
                
                // Extrai porta do endereço
                String[] addressParts = peerAddress.split(":");
                if (addressParts.length == 2) {
                    String host = addressParts[0];
                    int port = Integer.parseInt(addressParts[1]);
                    
                    String peerKey = peerName + "@" + peerAddress;
                    DiscoveredPeer peer = discoveredPeers.get(peerKey);
                    
                    if (peer == null) {
                        // Novo peer descoberto
                        peer = new DiscoveredPeer(peerName, host, port);
                        discoveredPeers.put(peerKey, peer);
                        listener.onPeerDiscovered(peer);
                        System.out.println("Peer descoberto: " + peer);
                    } else {
                        // Atualiza timestamp do peer existente
                        peer.updateLastSeen();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem de descoberta: " + e.getMessage());
        }
    }
    
    /**
     * Remove peers inativos (não vistos há mais de 30 segundos)
     */
    private void cleanupInactivePeers() {
        long currentTime = System.currentTimeMillis();
        long timeout = 30000; // 30 segundos
        
        Iterator<Map.Entry<String, DiscoveredPeer>> iterator = discoveredPeers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DiscoveredPeer> entry = iterator.next();
            DiscoveredPeer peer = entry.getValue();
            
            if (currentTime - peer.getLastSeen() > timeout) {
                iterator.remove();
                listener.onPeerLost(peer);
                System.out.println("Peer perdido: " + peer);
            }
        }
    }
    
    /**
     * Retorna lista de peers descobertos
     */
    public List<DiscoveredPeer> getDiscoveredPeers() {
        return new ArrayList<>(discoveredPeers.values());
    }
    
    /**
     * Verifica se um peer específico foi descoberto
     */
    public boolean isPeerDiscovered(String peerName) {
        return discoveredPeers.values().stream()
                .anyMatch(peer -> peer.getName().equals(peerName));
    }
}
