# Sistema de Chat P2P com Java ServerSocket

Um sistema de chat distribuído Peer-to-Peer (P2P) desenvolvido em Java que permite comunicação direta entre múltiplos usuários através de conexões TCP. O sistema inclui descoberta automática de peers na rede local usando multicast UDP.

## Funcionalidades

- **Chat em tempo real** entre múltiplos peers
- **Descoberta automática** de peers na rede local via multicast UDP
- **Histórico persistente** de mensagens salvo em arquivo
- **Conexões simultâneas** com múltiplos usuários
- **Interface de linha de comando** intuitiva
- **Relay de mensagens** para evitar loops infinitos
- **Detecção de desconexões** automática

## Arquitetura

O sistema é composto por cinco classes principais:

- **`Main.java`** - Interface principal e processamento de comandos
- **`Peer.java`** - Gerenciamento de conexões e comunicação
- **`Message.java`** - Estrutura de dados para mensagens
- **`PeerDiscovery.java`** - Descoberta automática de peers na rede
- **`ChatHistory.java`** - Gerenciamento do histórico de mensagens e persistência

## Instalação e Execução

### 1. Compilação
```bash
javac -d . src/main/websockets/*.java
```

### 2. Executar o programa
```bash
java websockets.Main
```

### 3. Primeira execução
Na primeira execução, você será solicitado a:
- Digitar seu nome de usuário
- Escolher uma porta para o seu peer

### 4. Execuções subsequentes
O sistema lembra suas configurações anteriores e as reutiliza automaticamente.

## Comandos Disponíveis

| Comando | Descrição |
|---------|-----------|
| `-help` | Mostra todos os comandos disponíveis |
| `-connect [IP] [PORT]` | Conecta a um peer específico |
| `-connect [USERNAME]` | Conecta a um peer descoberto pelo nome |
| `-list` | Lista todas as conexões ativas |
| `-send [mensagem]` | Envia mensagem para todos os peers conectados |
| `-history` | Mostra o histórico de mensagens |
| `-discover` | Lista peers descobertos na rede |
| `-ip` | Mostra informações de IP do host |
| `-files` | Mostra informações do arquivo de histórico |
| `-load` | Carrega e exibe o histórico completo |
| `-current` | Mostra o arquivo de chat atual |
| `exit` ou `quit` | Encerra o programa |

## Como Funciona

### Descoberta de Peers
- Cada peer se anuncia na rede local usando multicast UDP (endereço: `230.0.0.0:8888`)
- Peers descobertos são listados automaticamente
- Você pode conectar a qualquer peer descoberto usando apenas o nome de usuário

### Comunicação
- Conexões são estabelecidas via TCP usando ServerSocket
- Mensagens são serializadas e enviadas como objetos Java
- Sistema de relay evita loops infinitos de mensagens
- Cada mensagem possui ID único para controle de duplicatas

### Persistência
- Todas as mensagens são salvas automaticamente em `history.txt`
- Histórico é carregado automaticamente na inicialização
- Timestamps são adicionados a cada mensagem
- **`ChatHistory.java`** gerencia toda a persistência de forma organizada
- Suporte a carregamento de histórico existente
- Informações de usuário e porta são preservadas no arquivo

## Melhorias na Organização do Código

### Separação de Responsabilidades
- **`ChatHistory.java`** foi criada para centralizar todas as operações relacionadas ao histórico
- Melhor organização do código com responsabilidades bem definidas
- Facilita manutenção e extensão das funcionalidades de histórico

### Funcionalidades do ChatHistory
- Inicialização automática de arquivos de histórico
- Carregamento de mensagens existentes
- Adição de novas mensagens com timestamps
- Exibição do histórico de mensagens
- Informações sobre arquivos de histórico
- Leitura de informações de usuário para restauração de sessão

## Tecnologias Utilizadas

- **Java ServerSocket** - Comunicação TCP
- **Java MulticastSocket** - Descoberta de peers
- **Java Serialization** - Transmissão de objetos
- **Concurrent Collections** - Thread safety
- **ExecutorService** - Gerenciamento de threads
