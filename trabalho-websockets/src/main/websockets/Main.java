package websockets;

import java.net.*;
import java.util.Enumeration;
import java.util.Scanner;

public class Main {
    private static Peer peer;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.print("Digite a porta para este peer: ");
        
        int port;
        try {
            port = Integer.parseInt(scanner.nextLine());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        peer = new Peer(port);
        if (!peer.start()) {
            System.out.println("Erro ao iniciar o peer na porta " + port);
            return;
        }

        System.out.println("Peer iniciado na porta " + port);
        System.out.println("Digite -help para ver os comandos disponíveis");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }

            if (input.equals("exit") || input.equals("quit")) {
                System.out.println("Encerrando...");
                peer.stop();
                break;
            }

            processCommand(input);
        }

        scanner.close();
    }

    private static void processCommand(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0];

        switch (command) {
            case "-help":
                showHelp();
                break;
            case "-connect":
                if (parts.length != 3) {
                    System.out.println("Uso: -connect [IP_HOST] [PORT]");
                } else {
                    connectToPeer(parts[1], parts[2]);
                }
                break;
            case "-list":
                peer.listConnections();
                break;
            case "-send":
                if (parts.length < 2) {
                    System.out.println("Uso: -send [mensagem]");
                } else {
                    String message = input.substring(input.indexOf(' ') + 1);
                    peer.broadcastMessage(message);
                }
                break;
            case "-ip":
                showHostIP();
                break;
            default:
                System.out.println("Comando inválido. Digite -help para ver os comandos disponíveis");
        }
    }

    private static void showHelp() {
        System.out.println("\n=== Comandos Disponíveis ===");
        System.out.println("-help                     - Mostra esta ajuda");
        System.out.println("-connect [IP_HOST] [PORT] - Conecta a outro peer");
        System.out.println("-list                     - Lista conexões ativas");
        System.out.println("-send [mensagem]          - Envia mensagem para todos os peers conectados");
        System.out.println("-ip                       - Mostra o IP deste host");
        System.out.println("exit/quit                 - Encerra o programa");
        System.out.println("============================\n");
    }

    private static void connectToPeer(String host, String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            if (peer.connectToPeer(host, port)) {
                System.out.println("Conectado com sucesso ao peer " + host + ":" + port);
            } else {
                System.out.println("Falha ao conectar com o peer " + host + ":" + port);
            }
        } catch (NumberFormatException e) {
            System.out.println("Porta inválida: " + portStr);
        }
    }

    private static void showHostIP() {
        System.out.println("\n=== Informações do Host ===");
        
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            System.out.println("Host local: " + localhost.getHostAddress() + " (" + localhost.getHostName() + ")");
            
            System.out.println("\nTodos os IPs disponíveis:");
            
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    if (address instanceof Inet4Address && !address.isLinkLocalAddress()) {
                        System.out.println("  " + networkInterface.getDisplayName() + ": " + address.getHostAddress());
                    }
                }
            }
            
            if (peer != null) {
                System.out.println("\nPorta deste peer: " + peer.getPort());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
