package websockets;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class ChatHistory {
    private final String chatFileName;
    private final List<String> messageHistory;
    private final String username;
    private final int port;

    public ChatHistory(String username, int port) {
        this.username = username;
        this.port = port;
        this.messageHistory = new CopyOnWriteArrayList<>();
        this.chatFileName = initializeChatFile();
    }

    private String initializeChatFile() {
        try {
            String fileName = "history.txt";
            
            if (Files.exists(Paths.get(fileName))) {
                System.out.println("Continuando chat existente: " + fileName);
                loadExistingMessages(fileName);
            } else {
                String header = "=== Chat History for " + username + " (Port: " + port + ") ===\n";
                header += "Started: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n";
                header += "===============================================\n\n";
                Files.write(Paths.get(fileName), header.getBytes());
                System.out.println("Novo chat iniciado: " + fileName);
            }
            
            return fileName;
            
        } catch (IOException e) {
            System.err.println("Erro ao inicializar arquivo de chat: " + e.getMessage());
            return "history.txt";
        }
    }

    private void loadExistingMessages(String fileName) {
        try {
            Path filePath = Paths.get(fileName);
            if (!Files.exists(filePath)) {
                return;
            }

            Files.lines(filePath).forEach(line -> {
                if (!line.startsWith("===") && !line.startsWith("Started:") && 
                    !line.startsWith("===============================================") && 
                    !line.trim().isEmpty()) {
                    
                    if (line.startsWith("[") && line.contains("] ")) {
                        String message = line.substring(line.indexOf("] ") + 2);
                        messageHistory.add(message);
                    }
                }
            });
            
            System.out.println("Carregadas " + messageHistory.size() + " mensagens do histórico existente.");
            
        } catch (IOException e) {
            System.err.println("Erro ao carregar mensagens existentes: " + e.getMessage());
        }
    }

    public void addMessage(String message) {
        messageHistory.add(message);
        appendToChatFile(message);
    }

    private void appendToChatFile(String message) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String logEntry = "[" + timestamp + "] " + message + "\n";
            Files.write(Paths.get(chatFileName), logEntry.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Erro ao salvar mensagem no arquivo: " + e.getMessage());
        }
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

    public void loadChatHistory(String filename) {
        try {
            Path filePath = Paths.get(filename);
            if (!Files.exists(filePath)) {
                System.out.println("Arquivo não encontrado: " + filename);
                return;
            }

            System.out.println("\n=== Carregando Histórico de: " + filename + " ===");
            Files.lines(filePath).forEach(System.out::println);
            System.out.println("========================================\n");

        } catch (IOException e) {
            System.err.println("Erro ao carregar histórico: " + e.getMessage());
        }
    }

    public void listChatHistoryFiles() {
        try {
            Path historyFile = Paths.get("history.txt");
            if (!Files.exists(historyFile)) {
                System.out.println("Arquivo de histórico não existe.");
                return;
            }

            System.out.println("\n=== Arquivo de Histórico ===");
            String filename = historyFile.getFileName().toString();
            long size = Files.size(historyFile);
            String modified = Files.getLastModifiedTime(historyFile).toString().substring(0, 19);
            System.out.println(filename + " (" + size + " bytes, modificado: " + modified + ")");
            System.out.println("============================\n");

        } catch (IOException e) {
            System.err.println("Erro ao listar arquivo: " + e.getMessage());
        }
    }

    public static String[] readUserInfoFromHistory() {
        try {
            Path historyFile = Paths.get("history.txt");
            if (!Files.exists(historyFile)) {
                return null;
            }

            String firstLine = Files.lines(historyFile).findFirst().orElse("");
            if (firstLine.startsWith("=== Chat History for ")) {
                // Parse: === Chat History for username (Port: port) ===
                String content = firstLine.substring(21); // Remove "=== Chat History for "
                content = content.substring(0, content.length() - 4); // Remove " ==="
                
                if (content.contains(" (Port: ")) {
                    String username = content.substring(0, content.indexOf(" (Port: "));
                    String portStr = content.substring(content.indexOf(" (Port: ") + 8);
                    portStr = portStr.substring(0, portStr.length() - 1); // Remove ")"
                    
                    return new String[]{username, portStr};
                }
            }
            
        } catch (IOException e) {
            System.err.println("Erro ao ler informações do histórico: " + e.getMessage());
        }
        
        return null;
    }

    public String getChatFileName() {
        return chatFileName;
    }

    public List<String> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }

    public int getMessageCount() {
        return messageHistory.size();
    }
}
