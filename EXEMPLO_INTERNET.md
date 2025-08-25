# Exemplos de Uso do Chat P2P via Internet

## Cenário 1: Dois Usuários em Redes Diferentes

### Usuário 1: João (IP Público: 203.0.113.1)

1. **Descubra seu IP público**:
   ```bash
   # No Linux/Mac
   curl ifconfig.me
   
   # Ou acesse: https://whatismyipaddress.com/
   ```

2. **Configure port forwarding no roteador**:
   - Acesse o painel do roteador (geralmente 192.168.1.1)
   - Configure port forwarding da porta 8080 para o IP local do seu computador
   - Exemplo: Porta Externa: 8080 → IP Interno: 192.168.1.100 → Porta Interna: 8080

3. **Execute o chat**:
   ```bash
   java ChatCLI Joao 8080 203.0.113.1
   ```

### Usuário 2: Maria (IP Público: 203.0.113.2)

1. **Descubra seu IP público** (mesmo processo)

2. **Configure port forwarding** (mesmo processo)

3. **Execute o chat**:
   ```bash
   java ChatCLI Maria 8081 203.0.113.2
   ```

4. **Conecte ao João**:
   ```
   > connect 203.0.113.1 8080 Joao
   ```

5. **Envie uma mensagem**:
   ```
   > broadcast Olá João! Tudo bem?
   ```

## Cenário 2: Múltiplos Usuários

### Usuário 3: Pedro (IP Público: 203.0.113.3)

1. **Execute o chat**:
   ```bash
   java ChatCLI Pedro 8082 203.0.113.3
   ```

2. **Conecte ao João**:
   ```
   > connect 203.0.113.1 8080 Joao
   ```

3. **Conecte à Maria**:
   ```
   > connect 203.0.113.2 8081 Maria
   ```

4. **Envie mensagem para todos**:
   ```
   > broadcast Olá pessoal! Pedro aqui!
   ```

## Comandos Úteis para Uso via Internet

### Verificar Status
```
> status
```
Mostra seu nome, porta, IP público e número de conexões ativas.

### Listar Peers Conectados
```
> peers
```
Mostra todos os peers com quem você está conectado.

### Listar Peers Descobertos
```
> discover
```
Mostra peers descobertos automaticamente na rede.

### Enviar Mensagem Privada
```
> send Joao Olá João! Como vai?
```
Envia mensagem apenas para João.

### Enviar Mensagem para Todos
```
> broadcast Olá a todos! Como estão?
```
Envia mensagem para todos os peers conectados.

## Solução de Problemas

### Erro de Conexão Recusada
- Verifique se o peer está rodando
- Confirme se a porta está correta
- Verifique se o IP está correto
- Confirme se o port forwarding está configurado

### Mensagens Não Chegam
- Verifique se ambos os peers estão conectados
- Use o comando `peers` para confirmar conexões
- Verifique se não há firewall bloqueando

### Peer Não Aparece na Descoberta
- O sistema de descoberta funciona apenas em LAN
- Para internet, use sempre o comando `connect` manualmente
- Verifique se as portas UDP 8888 estão abertas

## Configurações de Rede

### Portas Recomendadas
- **Chat**: 8080-8090 (evite conflitos)
- **Descoberta**: 8888 (UDP, apenas para LAN)

### Firewall
- **Windows**: Adicione exceção para Java
- **Linux**: Configure iptables/ufw
- **Mac**: Configure Firewall do Sistema

### Roteador
- **Port Forwarding**: Configure para as portas do chat
- **DMZ**: Alternativa ao port forwarding (menos segura)
- **UPnP**: Alguns roteadores suportam configuração automática

## Exemplo Completo de Sessão

### Terminal 1 (João):
```bash
$ java ChatCLI Joao 8080 203.0.113.1
Peer Joao iniciado na porta 8080
Endereço local: 203.0.113.1:8080
Digite 'help' para ver os comandos disponíveis

> status
=== STATUS DO PEER ===
Nome: Joao
Porta: 8080
Endereço: 203.0.113.1:8080
Status: Ativo
Conexões ativas: 0
=====================

> peers
Nenhum peer conectado
```

### Terminal 2 (Maria):
```bash
$ java ChatCLI Maria 8081 203.0.113.2
Peer Maria iniciado na porta 8081
Endereço local: 203.0.113.2:8081
Digite 'help' para ver os comandos disponíveis

> connect 203.0.113.1 8080 Joao
Conectando a Joao em 203.0.113.1:8080...
Conectado a Joao (203.0.113.1:8080)

> broadcast Olá João! Tudo bem?
Mensagem enviada para 1 peer(s)
```

### Terminal 1 (João) - Recebe:
```
[SISTEMA] Maria conectou-se ao chat
[14:30:15] Maria: Olá João! Tudo bem?
```

### Terminal 1 (João) - Responde:
```
> broadcast Olá Maria! Tudo bem sim, e você?
Mensagem enviada para 1 peer(s)
```

### Terminal 2 (Maria) - Recebe:
```
[14:30:20] Joao: Olá Maria! Tudo bem sim, e você?
```

## Dicas Importantes

1. **Use IPs públicos reais**: Substitua 203.0.113.x pelos IPs reais
2. **Configure port forwarding**: Essencial para funcionar via internet
3. **Teste em LAN primeiro**: Confirme que funciona localmente
4. **Use portas diferentes**: Evite conflitos entre usuários
5. **Mantenha o chat aberto**: Conexões se perdem se fechar o terminal
6. **Verifique logs**: Use `history` para ver mensagens perdidas

## Suporte

Para problemas específicos de rede:
1. Teste conectividade: `ping <ip_do_peer>`
2. Teste porta: `telnet <ip_do_peer> <porta>`
3. Verifique logs do roteador
4. Confirme configurações de firewall
