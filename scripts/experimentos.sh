#!/usr/bin/env bash
# Gera as tabelas da Análise Experimental (saída em experimentos_resultado.md).
# Uso: scripts/experimentos.sh [diretorio_documentos]
#      scripts/experimentos.sh --gerar documentos 30   (cria textos de teste)
set -e
cd "$(dirname "$0")/.."
mkdir -p out
javac -d out *.java experimentos/Experimentos.java
java -cp out Experimentos "$@"
