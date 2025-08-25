import java.io.*;
import java.util.Scanner;

/**
 * Interface de linha de comando (CLI) para o sistema de chat P2P.
 * Permite interação do usuário via terminal com todos os comandos disponíveis.
 */
public class ChatCLI implements Peer.MessageListener, Peer.ConnectionStatusListener {
    
    private Peer peer;
    private Scanner scanner;
    private boolean running;
    
    /**
     * Construtor padrão (para uso em LAN)
     */
    public ChatCLI(String peerName, int port) {
        this(peerName, port, null);
    }
    
    /**
     * Construtor com IP personalizado (para uso via internet)
     */
    public ChatCLI(String peerName, int port, String customIP) {
        this.peer = new Peer(peerName, port, customIP);
        this.scanner = new Scanner(System.in);
        this.running = false;
        
        // Configura listeners
        this.peer.setMessageListener(this);
        this.peer.setConnectionStatusListener(this);
    }
    
    /**
     * Inicia a interface CLI
     */
    public void start() {
        try {
            // Inicia o peer
            peer.start();
            running = true;
            
            // Inicia loop principal de comandos
            commandLoop();
            
        } catch (Exception e) {
            System.err.println("Erro ao iniciar peer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    /**
     * Loop principal de comandos
     */
    private void commandLoop() {
        System.out.println("\n=== CHAT P2P - " + peer.getName() + " ===");
        System.out.println("Digite 'help' para ver os comandos disponíveis");
        System.out.println("Digite 'quit' para sair\n");
        
        while (running) {
            try {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) continue;
                
                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    break;
                }
                
                processCommand(input);
                
            } catch (Exception e) {
                System.err.println("Erro ao processar comando: " + e.getMessage());
            }
        }
    }
    
    /**
     * Processa comandos do usuário
     */
    private void processCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        switch (cmd) {
            case "help":
                showHelp();
                break;
                
            case "connect":
                handleConnect(args);
                break;
                
            case "disconnect":
                handleDisconnect(args);
                break;
                
            case "send":
                handleSend(args);
                break;
                
            case "broadcast":
                handleBroadcast(args);
                break;
                
            case "peers":
                peer.listConnectedPeers();
                break;
                
            case "discover":
                peer.listDiscoveredPeers();
                break;
                
            case "history":
                peer.showMessageHistory();
                break;
                
            case "status":
                showStatus();
                break;
                
            case "ping":
                handlePing(args);
                break;
                
            case "clear":
                clearScreen();
                break;
                
            default:
                // Se não for um comando, trata como mensagem de broadcast
                if (!args.isEmpty()) {
                    peer.broadcastMessage(command);
                } else {
                    System.out.println("Comando não reconhecido. Digite 'help' para ver os comandos disponíveis.");
                }
                break;
        }
    }
    
    /**
     * Trata comando de conexão
     */
    private void handleConnect(String args) {
        if (args.isEmpty()) {
            System.out.println("Uso: connect <host> <port> <nome>");
            System.out.println("Exemplo: connect 192.168.1.100 8080 Alice");
            return;
        }
        
        String[] parts = args.split("\\s+");
        if (parts.length != 3) {
            System.out.println("Uso: connect <host> <port> <nome>");
            return;
        }
        
        try {
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            String peerName = parts[2];
            
            if (peer.connectToPeer(host, port, peerName)) {
                System.out.println("Conectando a " + peerName + " em " + host + ":" + port + "...");
            }
        } catch (NumberFormatException e) {
            System.out.println("Porta deve ser um número válido");
        }
    }
    
    /**
     * Trata comando de desconexão
     */
    private void handleDisconnect(String args) {
        if (args.isEmpty()) {
            System.out.println("Uso: disconnect <nome>");
            return;
        }
        
        peer.disconnectFromPeer(args);
    }
    
    /**
     * Trata comando de envio de mensagem para peer específico
     */
    private void handleSend(String args) {
        if (args.isEmpty()) {
            System.out.println("Uso: send <nome> <mensagem>");
            return;
        }
        
        int firstSpace = args.indexOf(' ');
        if (firstSpace == -1) {
            System.out.println("Uso: send <nome> <mensagem>");
            return;
        }
        
        String peerName = args.substring(0, firstSpace);
        String message = args.substring(firstSpace + 1);
        
        peer.sendMessageToPeer(peerName, message);
    }
    
    /**
     * Trata comando de broadcast
     */
    private void handleBroadcast(String args) {
        if (args.isEmpty()) {
            System.out.println("Uso: broadcast <mensagem>");
            return;
        }
        
        peer.broadcastMessage(args);
    }
    
    /**
     * Trata comando de ping
     */
    private void handlePing(String args) {
        if (args.isEmpty()) {
            System.out.println("Uso: ping <nome>");
            return;
        }
        
        // Implementar ping se necessário
        System.out.println("Ping para " + args + " enviado");
    }
    
    /**
     * Mostra ajuda dos comandos
     */
    private void showHelp() {
        System.out.println("\n=== COMANDOS DISPONÍVEIS ===");
        System.out.println("help                    - Mostra esta ajuda");
        System.out.println("connect <host> <port> <nome> - Conecta a um peer");
        System.out.println("disconnect <nome>       - Desconecta de um peer");
        System.out.println("send <nome> <mensagem>  - Envia mensagem para peer específico");
        System.out.println("broadcast <mensagem>    - Envia mensagem para todos os peers");
        System.out.println("peers                   - Lista peers conectados");
        System.out.println("discover                - Lista peers descobertos");
        System.out.println("history                 - Mostra histórico de mensagens");
        System.out.println("status                  - Mostra status do peer");
        System.out.println("ping <nome>             - Envia ping para peer");
        System.out.println("clear                   - Limpa a tela");
        System.out.println("quit/exit               - Sai do chat");
        System.out.println("\nNOTA: Mensagens digitadas sem comando são enviadas como broadcast");
        System.out.println("================================\n");
    }
    
    /**
     * Mostra status do peer
     */
    private void showStatus() {
        System.out.println("\n=== STATUS DO PEER ===");
        System.out.println("Nome: " + peer.getName());
        System.out.println("Porta: " + peer.getPort());
        System.out.println("Endereço: " + peer.getLocalAddress());
        System.out.println("Status: " + (peer.isRunning() ? "Ativo" : "Inativo"));
        System.out.println("Conexões ativas: " + peer.getConnectionCount());
        System.out.println("=====================\n");
    }
    
    /**
     * Limpa a tela
     */
    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    
    /**
     * Limpa recursos
     */
    private void cleanup() {
        if (peer != null) {
            peer.stop();
        }
        if (scanner != null) {
            scanner.close();
        }
        running = false;
    }
    
    // Implementação das interfaces
    
    @Override
    public void onMessageReceived(Message message) {
        // Mensagens já são exibidas pelo peer
    }
    
    @Override
    public void onSystemMessage(String message) {
        System.out.println("[SISTEMA] " + message);
    }
    
    @Override
    public void onPeerConnected(String peerName) {
        System.out.println("[SISTEMA] " + peerName + " conectou-se ao chat");
    }
    
    @Override
    public void onPeerDisconnected(String peerName) {
        System.out.println("[SISTEMA] " + peerName + " desconectou-se do chat");
    }
    
    @Override
    public void onConnectionCountChanged(int count) {
        // Pode ser usado para atualizar interface se necessário
    }
    
    /**
     * Método principal
     */
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Uso: java ChatCLI <nome> <porta> [ip_publico]");
            System.out.println("Exemplos:");
            System.out.println("  java ChatCLI Alice 8080                    # Para uso em LAN");
            System.out.println("  java ChatCLI Alice 8080 203.0.113.1       # Para uso via internet");
            System.exit(1);
        }
        
        try {
            String peerName = args[0];
            int port = Integer.parseInt(args[1]);
            String customIP = args.length == 3 ? args[2] : null;
            
            if (port < 1024 || port > 65535) {
                System.out.println("Porta deve estar entre 1024 e 65535");
                System.exit(1);
            }
            
            ChatCLI cli = new ChatCLI(peerName, port, customIP);
            
            // Adiciona shutdown hook para limpeza
            Runtime.getRuntime().addShutdownHook(new Thread(cli::cleanup));
            
            cli.start();
            
        } catch (NumberFormatException e) {
            System.out.println("Porta deve ser um número válido");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erro ao iniciar chat: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
