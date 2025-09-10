package websockets;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeerDiscovery implements Runnable {

    private static final String MULTICAST_ADDRESS = "230.0.0.0";
    private static final int DISCOVERY_PORT = 8888;
    private static final int BROADCAST_INTERVAL = 5; 

    private final Peer peer;
    private final MulticastSocket socket;
    private final InetAddress group;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, String> discoveredPeers = new HashMap<>();

    public PeerDiscovery(Peer peer) throws IOException {
        this.peer = peer;
        socket = new MulticastSocket(DISCOVERY_PORT);
        group = InetAddress.getByName(MULTICAST_ADDRESS);
        socket.joinGroup(group);
    }

    @Override
    public void run() {
        startBroadcasting();
        listenForPeers();
    }

    private void startBroadcasting() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                String message = "PEER:" + peer.getUsername() + ":" + peer.getPort();
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, DISCOVERY_PORT);
                socket.send(packet);
            } catch (IOException e) {
                System.err.println("Erro ao broadcastar presença: " + e.getMessage());
            }
        }, 0, BROADCAST_INTERVAL, TimeUnit.SECONDS);
    }

    private void listenForPeers() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("PEER:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String username = parts[1];
                        int port = Integer.parseInt(parts[2]);

                        if (!username.equals(peer.getUsername())) {
                            String address = packet.getAddress().getHostAddress() + ":" + port;
                            if (!discoveredPeers.containsKey(username)) {
                                System.out.println("\nPeer descoberto: " + username + " em " + address);
                                System.out.print("> ");
                            }
                            discoveredPeers.put(username, address);
                        }
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("Erro ao receber broadcast: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        scheduler.shutdownNow();
        if (socket != null && !socket.isClosed()) {
            try {
                socket.leaveGroup(group);
            } catch (IOException e) {
                System.err.println("Erro ao sair do grupo multicast: " + e.getMessage());
            }
            socket.close();
        }
    }

    public Map<String, String> getDiscoveredPeers() {
        return new HashMap<>(discoveredPeers);
    }
}
