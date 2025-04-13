# Makefile para proyecto Java

SRC_DIR := src
BIN_DIR := bin
LIB_DIR := lib
PDF_DIR := pdfFiles
MAIN_SERVER_CLASS := WebServer  # Ajusta esto a tu clase principal
MAIN_CLIENT_CLASS := Client

# Encuentra todos los .java en src
SOURCES := $(shell find $(SRC_DIR) -name "*.java")

# Encuentra todos los .jar y únelos con ":"
LIB_JARS := $(wildcard $(LIB_DIR)/*.jar)
JAR_PATH := $(subst $(space),:,$(strip $(LIB_JARS)))

# Espacio como variable (para usarlo en subst)
space :=

# Classpath final: jars + bin
CLASSPATH=lib/fontbox-3.0.4.jar:lib/gson-2.10.1.jar:lib/pdfbox-3.0.4.jar:lib/pdfbox-app-3.0.4.jar:lib/pdfbox-tools-3.0.4.jar:lib/preflight-3.0.4.jar:lib/preflight-app-3.0.4.jar:lib/xmpbox-3.0.4.jar:bin


.PHONY: all run clean

all:
	javac -cp "$(CLASSPATH)" -d bin src/Server.java src/WebServer.java src/GoogleCloudService.java
	javac -d bin src/Client.java
	@echo "Compilación completada."

run:
	java -cp "$(CLASSPATH)" $(MAIN_SERVER_CLASS)

run_client:
	java -cp "$(CLASSPATH)" $(MAIN_CLIENT_CLASS)

clean:
	rm -rf $(BIN_DIR)/*
	rm -rf $(PDF_DIR)/*.pdf
	@echo "Limpieza completada."
