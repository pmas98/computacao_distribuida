#!/bin/bash

# Script de Demonstração do Sistema de Chat P2P
# Este script demonstra o funcionamento do chat P2P genérico

echo "=== DEMONSTRAÇÃO DO SISTEMA DE CHAT P2P ==="
echo ""

# Verificar se Java está instalado
if ! command -v java &> /dev/null; then
    echo "ERRO: Java não está instalado!"
    echo "Instale o JDK primeiro e tente novamente."
    exit 1
fi

# Verificar se javac está instalado
if ! command -v javac &> /dev/null; then
    echo "ERRO: Java Compiler (javac) não está instalado!"
    echo "Instale o JDK completo e tente novamente."
    exit 1
fi

echo "✓ Java encontrado: $(java -version 2>&1 | head -n 1)"
echo "✓ Java Compiler encontrado"
echo ""

# Limpar arquivos antigos
echo "Limpando arquivos compilados antigos..."
rm -f *.class chat-p2p.jar
echo "✓ Limpeza concluída"
echo ""

# Compilar o projeto
echo "Compilando o projeto..."
javac -encoding UTF-8 *.java
if [ $? -ne 0 ]; then
    echo "ERRO: Falha na compilação!"
    exit 1
fi
echo "✓ Compilação concluída com sucesso"
echo ""

echo "=== INSTRUÇÕES PARA USO ==="
echo ""
echo "Para usar o chat P2P:"
echo ""
echo "1. COMPILAR O PROJETO:"
echo "   make compile"
echo "   # ou"
echo "   javac -encoding UTF-8 *.java"
echo ""
echo "2. EXECUTAR EM LAN (rede local):"
echo "   java ChatCLI SeuNome 8080"
echo ""
echo "3. EXECUTAR VIA INTERNET:"
echo "   java ChatCLI SeuNome 8080 SeuIPPublico"
echo "   # Exemplo: java ChatCLI Joao 8080 203.0.113.1"
echo ""
echo "4. CONECTAR A OUTRO PEER:"
echo "   > connect 192.168.1.100 8080 NomeDoPeer"
echo "   # ou via internet:"
echo "   > connect 203.0.113.2 8080 NomeDoPeer"
echo ""
echo "5. COMANDOS DISPONÍVEIS:"
echo "   - 'help' para ver todos os comandos"
echo "   - 'connect <host> <port> <nome>' para conectar a um peer"
echo "   - 'broadcast <mensagem>' para enviar mensagem para todos"
echo "   - 'send <nome> <mensagem>' para enviar mensagem específica"
echo "   - 'peers' para listar peers conectados"
echo "   - 'discover' para ver peers descobertos"
echo "   - 'history' para ver histórico de mensagens"
echo "   - 'status' para ver status do peer"
echo "   - 'quit' para sair"
echo ""
echo "=== EXEMPLO DE USO VIA INTERNET ==="
echo ""
echo "PEER 1 (João - IP: 203.0.113.1):"
echo "  java ChatCLI Joao 8080 203.0.113.1"
echo ""
echo "PEER 2 (Maria - IP: 203.0.113.2):"
echo "  java ChatCLI Maria 8081 203.0.113.2"
echo ""
echo "PEER 2 conecta ao PEER 1:"
echo "  > connect 203.0.113.1 8080 Joao"
echo ""
echo "Enviar mensagem:"
echo "  > broadcast Olá João! Como vai?"
echo ""
echo "=== IMPORTANTE ==="
echo "- Para uso via internet, você precisa do seu IP público"
echo "- Configure port forwarding no seu roteador se necessário"
echo "- Use portas acima de 1024 (recomendado: 8080-8090)"
echo "- O sistema funciona tanto em LAN quanto via internet"
echo ""
echo "=== DEMONSTRAÇÃO CONCLUÍDA ==="
echo "O sistema está pronto para uso!"
