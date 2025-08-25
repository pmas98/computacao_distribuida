#!/bin/bash

# Script de debug para testar conexões do Chat P2P
# Ajuda a identificar problemas de conectividade

echo "=== DEBUG DE CONEXÕES - CHAT P2P ==="
echo ""

# Verifica se o Java está instalado
if ! command -v java &> /dev/null; then
    echo "ERRO: Java não está instalado!"
    exit 1
fi

# Verifica se o compilador Java está instalado
if ! command -v javac &> /dev/null; then
    echo "ERRO: Java Compiler (javac) não está instalado!"
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

# Função para testar conectividade básica
test_connectivity() {
    local host=$1
    local port=$2
    local name=$3
    
    echo "=== TESTANDO CONECTIVIDADE ==="
    echo "Host: $host"
    echo "Porta: $port"
    echo "Nome: $name"
    echo ""
    
    # Testa se a porta está aberta
    echo "Testando se a porta $port está acessível..."
    if timeout 5 bash -c "</dev/tcp/$host/$port" 2>/dev/null; then
        echo "✓ Porta $port está acessível em $host"
    else
        echo "✗ Porta $port NÃO está acessível em $host"
        echo "  Possíveis causas:"
        echo "  - Peer não está rodando"
        echo "  - Firewall bloqueando a porta"
        echo "  - IP incorreto"
        echo "  - Porta incorreta"
    fi
    echo ""
}

# Função para iniciar peer em background
start_peer() {
    local name=$1
    local port=$2
    local ip=$3
    local log_file="${name}_debug.log"
    
    echo "Iniciando peer '$name' na porta $port (IP: $ip)..."
    
    if [ -n "$ip" ]; then
        java ChatCLI "$name" "$port" "$ip" > "$log_file" 2>&1 &
    else
        java ChatCLI "$name" "$port" > "$log_file" 2>&1 &
    fi
    
    local pid=$!
    echo "Peer '$name' iniciado com PID: $pid"
    echo "Log salvo em: $log_file"
    echo ""
    
    # Aguarda um pouco para o peer inicializar
    sleep 3
    
    # Verifica se o peer está rodando
    if kill -0 $pid 2>/dev/null; then
        echo "✓ Peer '$name' está rodando"
    else
        echo "✗ Peer '$name' falhou ao iniciar"
        echo "Verifique o log: $log_file"
    fi
    echo ""
    
    return $pid
}

# Função para mostrar logs
show_logs() {
    local name=$1
    local log_file="${name}_debug.log"
    
    if [ -f "$log_file" ]; then
        echo "=== LOG DO PEER '$name' ==="
        echo "Últimas 20 linhas:"
        tail -20 "$log_file"
        echo ""
    else
        echo "Log não encontrado para '$name'"
    fi
}

# Função para parar peer
stop_peer() {
    local name=$1
    local pid=$2
    
    if [ -n "$pid" ] && kill -0 $pid 2>/dev/null; then
        echo "Parando peer '$name' (PID: $pid)..."
        kill $pid
        sleep 1
        
        if kill -0 $pid 2>/dev/null; then
            echo "Forçando parada do peer '$name'..."
            kill -9 $pid
        fi
        
        echo "Peer '$name' parado"
    else
        echo "Peer '$name' não está rodando"
    fi
    echo ""
}

# Menu principal
while true; do
    echo "Escolha uma opção:"
    echo "1. Testar conectividade de um host/porta"
    echo "2. Iniciar peer para teste"
    echo "3. Mostrar logs de um peer"
    echo "4. Parar peer"
    echo "5. Teste completo (2 peers)"
    echo "6. Limpar logs"
    echo "7. Sair"
    echo ""
    read -p "Opção: " choice
    
    case $choice in
        1)
            read -p "Host/IP: " host
            read -p "Porta: " port
            read -p "Nome (opcional): " name
            test_connectivity "$host" "$port" "$name"
            ;;
        2)
            read -p "Nome do peer: " name
            read -p "Porta: " port
            read -p "IP (opcional, Enter para auto-detect): " ip
            start_peer "$name" "$port" "$ip"
            ;;
        3)
            read -p "Nome do peer: " name
            show_logs "$name"
            ;;
        4)
            read -p "Nome do peer: " name
            read -p "PID (se conhecido, Enter para procurar): " pid
            if [ -z "$pid" ]; then
                pid=$(pgrep -f "ChatCLI $name")
            fi
            stop_peer "$name" "$pid"
            ;;
        5)
            echo "=== TESTE COMPLETO ==="
            echo "Iniciando teste com 2 peers..."
            echo ""
            
            # Inicia primeiro peer
            start_peer "alice" "8080" ""
            ALICE_PID=$?
            
            # Inicia segundo peer
            start_peer "bob" "8081" ""
            BOB_PID=$?
            
            echo "Teste iniciado!"
            echo "Para parar: kill $ALICE_PID $BOB_PID"
            echo "Para ver logs: tail -f alice_debug.log ou tail -f bob_debug.log"
            echo ""
            ;;
        6)
            echo "Removendo logs de debug..."
            rm -f *_debug.log
            echo "Logs removidos!"
            echo ""
            ;;
        7)
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
