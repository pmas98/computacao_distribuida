# Makefile para o Sistema de Chat P2P
# Autor: Sistema de Chat P2P
# Data: 2024

# Configurações
JAVAC = javac
JAVA = java
JAR = jar
SRC_DIR = .
BIN_DIR = .
MAIN_CLASS = ChatCLI

# Arquivos fonte
SOURCES = $(wildcard $(SRC_DIR)/*.java)
CLASSES = $(SOURCES:$(SRC_DIR)/%.java=$(BIN_DIR)/%.class)

# Flags de compilação
JAVAC_FLAGS = -encoding UTF-8 -sourcepath $(SRC_DIR) -d $(BIN_DIR)

# Nome do arquivo JAR
JAR_FILE = chat-p2p.jar

# Regra padrão
all: compile

# Compilar todas as classes
compile: $(CLASSES)

$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	@echo "Compilando $<..."
	@$(JAVAC) $(JAVAC_FLAGS) $<
	@echo "Compilação concluída!"

# Criar arquivo JAR executável
jar: compile
	@echo "Criando arquivo JAR..."
	@echo "Main-Class: $(MAIN_CLASS)" > manifest.txt
	@$(JAR) cfm $(JAR_FILE) manifest.txt $(BIN_DIR)/*.class
	@rm manifest.txt
	@echo "JAR criado: $(JAR_FILE)"

# Executar o chat
run: compile
	@echo "Iniciando chat P2P..."
	@echo "Uso: java $(MAIN_CLASS) <nome> <porta> [ip_publico]"
	@echo "Exemplos:"
	@echo "  java $(MAIN_CLASS) MeuNome 8080                    # Para uso em LAN"
	@echo "  java $(MAIN_CLASS) MeuNome 8080 203.0.113.1       # Para uso via internet"

# Executar com IP público (para uso via internet)
run-internet: compile
	@echo "Iniciando chat P2P com IP público..."
	@echo "Uso: java $(MAIN_CLASS) <nome> <porta> <ip_publico>"
	@echo "Exemplo: java $(MAIN_CLASS) MeuNome 8080 203.0.113.1"
	@echo ""
	@echo "IMPORTANTE: Substitua 203.0.113.1 pelo seu IP público real"

# Limpar arquivos compilados
clean:
	@echo "Limpando arquivos compilados..."
	@rm -f $(BIN_DIR)/*.class
	@rm -f $(JAR_FILE)
	@echo "Limpeza concluída!"

# Limpar arquivos compilados antigos
clean-old:
	@echo "Removendo arquivos .class antigos..."
	@rm -f *.class
	@echo "Limpeza concluída!"

# Mostrar ajuda
help:
	@echo "=== Makefile para Sistema de Chat P2P ==="
	@echo ""
	@echo "Comandos disponíveis:"
	@echo "  make compile      - Compila todas as classes Java"
	@echo "  make jar         - Cria arquivo JAR executável"
	@echo "  make run         - Mostra instruções de execução"
	@echo "  make run-internet - Mostra instruções para uso via internet"
	@echo "  make clean       - Remove arquivos compilados"
	@echo "  make clean-old   - Remove arquivos .class antigos"
	@echo "  make help        - Mostra esta ajuda"
	@echo ""
	@echo "Para executar manualmente:"
	@echo "  java ChatCLI <nome> <porta> [ip_publico]"
	@echo "  Exemplos:"
	@echo "    java ChatCLI MeuNome 8080                    # Para uso em LAN"
	@echo "    java ChatCLI MeuNome 8080 203.0.113.1       # Para uso via internet"
	@echo ""

# Verificar se Java está instalado
check-java:
	@echo "Verificando instalação do Java..."
	@$(JAVA) -version || (echo "Java não encontrado. Instale o JDK primeiro." && exit 1)
	@echo "Java encontrado!"

# Instalar dependências (se necessário)
install-deps: check-java
	@echo "Dependências verificadas. Java está instalado."

# Regra para desenvolvimento
dev: clean compile
	@echo "Ambiente de desenvolvimento configurado!"

# Regra para produção
prod: clean jar
	@echo "Build de produção concluído!"

.PHONY: all compile jar run run-internet clean clean-old help check-java install-deps dev prod
