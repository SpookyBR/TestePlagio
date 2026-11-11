#!/usr/bin/env bash
# Uso: scripts/executar.sh <diretorio_documentos> <limiar> <modo> [args]
# Ex.: scripts/executar.sh documentos 0.7 lista
set -e
cd "$(dirname "$0")/.."
javac *.java
java Main "$@"
