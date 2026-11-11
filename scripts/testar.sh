#!/usr/bin/env bash
# Compila tudo e roda os testes das estruturas (HashTable e AVLTree).
set -e
cd "$(dirname "$0")/.."
mkdir -p out
javac -d out *.java testes/TesteEstruturas.java
java -cp out TesteEstruturas
