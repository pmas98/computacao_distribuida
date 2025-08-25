# Sistema de Chat P2P com Múltiplos Usuários

## Descrição

Este projeto implementa um sistema de chat descentralizado baseado no protocolo P2P (peer-to-peer), permitindo a comunicação entre múltiplos usuários sem a necessidade de um servidor central. O sistema é desenvolvido em Java e utiliza sockets TCP/IP para comunicação entre peers.

## Características Principais

- **Conexões Múltiplas**: Suporte a múltiplas conexões simultâneas entre peers
- **Identificação de Usuários**: Exibição do nome do usuário remetente com cada mensagem
- **Mensagens em Broadcast**: Sistema eficiente de envio de mensagens para todos os peers conectados
- **Histórico de Mensagens**: Registro e exibição de todas as mensagens trocadas durante a sessão
- **Descoberta Automática de Peers**: Sistema UDP para descoberta automática de outros peers na rede
- **Encerramento Seguro**: Fechamento correto de todas as conexões ao sair do sistema

## Arquitetura do Sistema

### Componentes Principais

1. **Message.java**: Classe que representa mensagens trocadas entre peers
   - Suporte a diferentes tipos de mensagem (CHAT, CONNECT, DISCONNECT, PING, PONG)
   - Timestamp automático e formatação para exibição
   - Implementação de Serializable para transmissão via sockets

2. **PeerConnection.java**: Gerencia conexões individuais entre peers
   - Estabelecimento de conexões TCP
   - Envio e recebimento de mensagens
   - Tratamento de erros e reconexão automática

3. **PeerDiscovery.java**: Sistema de descoberta automática de peers
   - Broadcast UDP para encontrar peers ativos na rede
   - Manutenção de lista de peers descobertos
   - Limpeza automática de peers inativos

4. **Peer.java**: Classe principal que coordena todas as funcionalidades
   - Gerenciamento de múltiplas conexões
   - Servidor para aceitar conexões de outros peers
   - Coordenação entre descoberta e comunicação

5. **ChatCLI.java**: Interface de linha de comando para interação do usuário
   - Comandos intuitivos para todas as operações
   - Tratamento de erros e validação de entrada
   - Interface responsiva e fácil de usar

### Fluxo de Funcionamento

1. **Inicialização**: Peer inicia servidor TCP e sistema de descoberta UDP
2. **Descoberta**: Broadcast periódico para encontrar outros peers na rede
3. **Conexão**: Estabelecimento de conexões TCP com peers descobertos
4. **Comunicação**: Troca de mensagens através das conexões estabelecidas
5. **Encerramento**: Fechamento seguro de todas as conexões e recursos

## Requisitos do Sistema

- **Java**: JDK 8 ou superior
- **Sistema Operacional**: Windows, Linux ou macOS
- **Rede**: Acesso à rede local para comunicação entre peers

## Instalação e Configuração

### 1. Verificar Java

```bash
java -version
javac -version
```

### 2. Compilar o Projeto

```bash
# Usando Makefile
make compile

# Ou manualmente
javac -encoding UTF-8 *.java
```

### 3. Executar

```bash
# Usando Makefile
make run          # Mostra instruções de uso
make run-internet # Mostra instruções para uso via internet

# Ou manualmente
java ChatCLI <nome> <porta> [ip_publico]
java ChatCLI Joao 8080                    # Para uso em LAN
java ChatCLI Joao 8080 203.0.113.1       # Para uso via internet
```

## Uso do Sistema

### Comandos Disponíveis

| Comando | Descrição | Exemplo |
|---------|-----------|---------|
| `help` | Mostra ajuda dos comandos | `help` |
| `connect <host> <port> <nome>` | Conecta a um peer específico | `connect 192.168.1.100 8080 Alice` |
| `disconnect <nome>` | Desconecta de um peer | `disconnect Alice` |
| `send <nome> <mensagem>` | Envia mensagem para peer específico | `send Alice Olá!` |
| `broadcast <mensagem>` | Envia mensagem para todos os peers | `broadcast Olá a todos!` |
| `peers` | Lista peers conectados | `peers` |
| `discover` | Lista peers descobertos | `discover` |
| `history` | Mostra histórico de mensagens | `history` |
| `status` | Mostra status do peer | `status` |
| `clear` | Limpa a tela | `clear` |
| `quit/exit` | Sai do chat | `quit` |

### Exemplo de Uso

#### Uso em LAN (Rede Local)

1. **Terminal 1 - João**:
   ```bash
   java ChatCLI Joao 8080
   ```

2. **Terminal 2 - Maria**:
   ```bash
   java ChatCLI Maria 8081
   ```

3. **Conectar Maria a João**:
   ```
   > connect 127.0.0.1 8080 Joao
   ```

#### Uso via Internet

1. **Peer 1 - João (IP: 203.0.113.1)**:
   ```bash
   java ChatCLI Joao 8080 203.0.113.1
   ```

2. **Peer 2 - Maria (IP: 203.0.113.2)**:
   ```bash
   java ChatCLI Maria 8081 203.0.113.2
   ```

3. **Conectar Maria a João**:
   ```
   > connect 203.0.113.1 8080 Joao
   ```

4. **Enviar mensagem**:
   ```
   > broadcast Olá a todos!
   ```

## Demonstração

Para uma demonstração do sistema:

```bash
# Executar script de demonstração
./demo.sh

# Ou usar Makefile
make run
make run-internet
```

**📖 Veja [EXEMPLO_INTERNET.md](EXEMPLO_INTERNET.md) para exemplos práticos de uso via internet!**

## Estrutura de Arquivos

```
.
├── Message.java              # Classe de mensagens
├── PeerConnection.java       # Gerenciamento de conexões
├── PeerDiscovery.java        # Descoberta automática de peers
├── Peer.java                 # Classe principal do peer
├── ChatCLI.java             # Interface de linha de comando
├── Makefile                 # Script de build e execução
├── demo.sh                  # Script de demonstração
├── test_chat.sh             # Script de teste automatizado
├── debug_connection.sh      # Script de debug para conexões
├── EXEMPLO_INTERNET.md      # Exemplos práticos de uso via internet
└── README.md                # Este arquivo
```

## Decisões Técnicas

### 1. Protocolo de Comunicação
- **TCP/IP**: Escolhido para garantir entrega confiável de mensagens
- **UDP**: Utilizado apenas para descoberta de peers (broadcast)

### 2. Arquitetura P2P
- **Descentralizada**: Sem servidor central, todos os peers são iguais
- **Escalável**: Suporte a múltiplas conexões simultâneas
- **Resiliente**: Funciona mesmo se alguns peers falharem

### 3. Gerenciamento de Conexões
- **Thread Pool**: ExecutorService para gerenciar múltiplas conexões
- **ConcurrentHashMap**: Estruturas de dados thread-safe
- **AtomicBoolean**: Controle de estado thread-safe

### 4. Interface CLI
- **Comandos Intuitivos**: Fácil de usar e lembrar
- **Validação**: Verificação de parâmetros e tratamento de erros
- **Responsiva**: Feedback imediato para todas as operações

## Dificuldades Encontradas

### 1. Sincronização de Threads
- **Problema**: Múltiplas threads acessando recursos compartilhados
- **Solução**: Uso de estruturas de dados thread-safe e sincronização adequada

### 2. Descoberta de Peers
- **Problema**: Implementar descoberta automática sem servidor central
- **Solução**: Sistema de broadcast UDP com limpeza periódica de peers inativos

### 3. Gerenciamento de Conexões
- **Problema**: Manter múltiplas conexões ativas e detectar falhas
- **Solução**: Thread pool dedicado e verificação periódica de conectividade

### 4. Interface CLI
- **Problema**: Criar interface intuitiva para todas as funcionalidades
- **Solução**: Comandos claros e sistema de ajuda integrado

### 5. Tratamento de Erros de Conexão
- **Problema**: Conexões sendo resetadas abruptamente ("Connection reset")
- **Solução**: Implementação de timeouts, validação de sockets e tratamento específico de exceções de rede

## Melhorias Futuras

1. **Criptografia**: Implementar criptografia end-to-end para mensagens
2. **Persistência**: Salvar histórico de mensagens em arquivo
3. **Interface Gráfica**: Desenvolver interface gráfica além da CLI
4. **Grupos**: Suporte a salas de chat e grupos
5. **Arquivos**: Compartilhamento de arquivos entre peers
6. **NAT Traversal**: Melhorar conectividade através de NATs

## Compilação e Execução

### Compilar
```bash
make compile
# ou
javac -encoding UTF-8 *.java
```

### Executar
```bash
make run
make run-internet
# ou diretamente
java ChatCLI <nome> <porta> [ip]
```

### Teste Automatizado
```bash
./test_chat.sh
```

### Debug de Conexões
```bash
./debug_connection.sh
```

### Criar JAR
```bash
make jar
java -jar chat-p2p.jar Joao 8080
```

### Limpar
```bash
make clean
```

## Suporte

Para dúvidas ou problemas:
1. Verifique se o Java está instalado corretamente
2. Confirme que as portas não estão sendo usadas por outros serviços
3. Verifique se o firewall não está bloqueando as conexões
4. Use o comando `help` para ver todos os comandos disponíveis

### Solução de Problemas Comuns

#### Erro "Connection reset"
Este erro ocorre quando uma conexão é interrompida abruptamente. As melhorias implementadas incluem:
- Timeouts configuráveis para evitar travamentos
- Tratamento específico de exceções de rede
- Validação de sockets antes do processamento
- Processamento assíncrono de conexões

#### Erro "Erro ao configurar streams: null"
Este erro foi corrigido com:
- Validação de parâmetros antes da criação de conexões
- Verificação de sockets válidos
- Timeouts de conexão (10s) e leitura (30s)
- Mensagens de erro mais informativas
- Tratamento robusto de falhas de I/O

#### Peer não é descoberto
- Verifique se ambos os peers estão na mesma rede
- Confirme que as portas de descoberta (8888) não estão bloqueadas
- Use o comando `discover` para verificar peers disponíveis

#### Falha na conexão
- Verifique se o peer de destino está rodando
- Confirme o IP e porta corretos
- Use `connect <host> <port> <nome>` para conectar manualmente

## Licença

Este projeto foi desenvolvido para fins educacionais e de demonstração.
