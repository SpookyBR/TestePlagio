# Verificador de Similaridade de Textos

Projeto de Projeto e Análise de Algoritmos II — Universidade Presbiteriana Mackenzie.

Compara documentos de texto (normalização, tabela hash por documento, similaridade entre pares)
e guarda os resultados em uma árvore AVL ordenada pela similaridade.

## Como compilar e executar (JDK 17+)

```bash
javac *.java
java Main documentos 0.75 lista
java Main documentos 0.8 topK 5
java Main documentos 0.0 busca doc1.txt doc4.txt
```

A saída vai para o terminal e para `resultado.txt`.

## Estrutura

```
.
├── Main.java                  # ponto de entrada (cabeçalho com nome e RA do grupo)
├── Documento.java             # leitura, normalização e HashTable de frequências
├── ComparadorDeDocumentos.java# métrica de similaridade
├── HashTable.java             # tabela hash manual (hashDivisao e hashMultiplicativo)
├── AVLTree.java               # AVL com lista de Resultado por nó e contagem de rotações
├── RegistroRotacoes.java      # contadores de rotações simples/duplas
├── Resultado.java             # par de documentos + similaridade
├── documentos/                # textos usados nos testes (entregar junto)
├── relatorio/                 # relatório técnico em PDF (entregar junto)
├── testes/
│   └── TesteEstruturas.java   # testes de HashTable e AVLTree (não precisa ir no .zip)
└── scripts/
    ├── testar.sh              # compila e roda os testes
    └── executar.sh            # compila e executa o programa
```

Os arquivos `.java` ficam na raiz de propósito: a correção roda `javac *.java && java Main ...`.

## Testes das estruturas

```bash
scripts/testar.sh
```

## Entrega (.zip)

Código-fonte (`*.java`), relatório em PDF e pasta `documentos/`.
