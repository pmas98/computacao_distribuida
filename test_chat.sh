#!/bin/bash

# Script de teste para o Chat P2P
# Demonstra como iniciar múltiplos peers e testar conexões

echo "=== TESTE DO CHAT P2P ==="
echo "Este script demonstra como usar o chat P2P"
echo ""

# Verifica se o Java está instalado
if ! command -v java &> /dev/null; then
    echo "ERRO: Java não está instalado!"
    echo "Instale o Java OpenJDK ou Oracle JDK"
    exit 1
fi

# Verifica se o compilador Java está instalado
if ! command -v javac &> /dev/null; then
    echo "ERRO: Java Compiler (javac) não está instalado!"
    echo "Instale o Java OpenJDK ou Oracle JDK"
    exit 1
fi

# Compila o projeto
echo "Compilando o projeto..."
javac -encoding UTF-8 *.java

if [ $? -ne 0 ]; then
    echo "ERRO: Falha na compilação!"
    exit 1
fi

echo "Compilação concluída com sucesso!"
echo ""

# Função para mostrar instruções
show_instructions() {
    echo "=== INSTRUÇÕES DE USO ==="
    echo "1. Abra um terminal e execute: java ChatCLI <nome> <porta> [ip]"
    echo "   Exemplo: java ChatCLI alice 8080"
    echo ""
    echo "2. Em outro terminal, execute: java ChatCLI <nome2> <porta2> [ip2]"
    echo "   Exemplo: java ChatCLI bob 8081"
    echo ""
    echo "3. Use o comando 'connect' para conectar os peers:"
    echo "   No terminal do alice: connect 127.0.0.1 8081 bob"
    echo "   No terminal do bob: connect 127.0.0.1 8080 alice"
    echo ""
    echo "4. Use 'send' para enviar mensagens:"
    echo "   send bob Olá, como vai?"
    echo ""
    echo "5. Use 'broadcast' para enviar para todos:"
    echo "   broadcast Mensagem para todos!"
    echo ""
    echo "6. Use 'peers' para ver conexões ativas"
    echo "7. Use 'discover' para ver peers na rede"
    echo "8. Use 'quit' para sair"
    echo ""
}

# Função para executar teste automático
run_auto_test() {
    echo "=== TESTE AUTOMÁTICO ==="
    echo "Iniciando teste com 2 peers..."
    echo ""
    
    # Inicia o primeiro peer em background
    echo "Iniciando peer 'alice' na porta 8080..."
    java ChatCLI alice 8080 > alice.log 2>&1 &
    ALICE_PID=$!
    sleep 2
    
    # Inicia o segundo peer em background
    echo "Iniciando peer 'bob' na porta 8081..."
    java ChatCLI bob 8081 > bob.log 2>&1 &
    BOB_PID=$!
    sleep 2
    
    echo "Ambos os peers foram iniciados!"
    echo "Verifique os logs: alice.log e bob.log"
    echo ""
    echo "Para parar o teste, execute:"
    echo "kill $ALICE_PID $BOB_PID"
    echo ""
}

# Menu principal
while true; do
    echo "Escolha uma opção:"
    echo "1. Mostrar instruções de uso"
    echo "2. Executar teste automático (2 peers)"
    echo "3. Sair"
    echo ""
    read -p "Opção: " choice
    
    case $choice in
        1)
            show_instructions
            ;;
        2)
            run_auto_test
            ;;
        3)
            echo "Saindo..."
            exit 0
            ;;
        *)
            echo "Opção inválida!"
            ;;
    esac
    
    echo ""
    read -p "Pressione Enter para continuar..."
    echo ""
done
