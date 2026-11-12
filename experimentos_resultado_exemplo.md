# Resultados dos experimentos

- Corpus dos experimentos 1 e 2: documentos reais de `docs_gerados` (30 arquivos)
- Java: 21.0.12.1 | processadores: 2
- Tempos: média de 5 execuções após aquecimento.

## Experimento 1: distribuição das chaves nas duas funções de dispersão

Cada conjunto de palavras é inserido em tabelas de capacidade fixa (sem redimensionar), com a mesma capacidade para as duas funções. `α` é o fator de carga alvo (elementos/buckets). Colisões = elementos − buckets ocupados. σ é o desvio padrão do tamanho das cadeias; σ esperado é √α, o valor de uma dispersão uniforme ideal (quanto mais perto, melhor).

| Conjunto | Palavras | α alvo | Buckets (m) | Função | Ocupados | Vazios | Colisões | Maior cadeia | σ | σ esperado |
|---|---|---|---|---|---|---|---|---|---|---|
| vocabulário do corpus | 198 | 0.5 | 397 | hashDivisao | 157 | 60.5% | 41 | 4 | 0.71 | 0.71 |
| vocabulário do corpus | 198 | 0.5 | 397 | hashMultiplicativo | 155 | 61.0% | 43 | 4 | 0.72 | 0.71 |
| vocabulário do corpus | 198 | 1.0 | 199 | hashDivisao | 122 | 38.7% | 76 | 5 | 1.00 | 1.00 |
| vocabulário do corpus | 198 | 1.0 | 199 | hashMultiplicativo | 124 | 37.7% | 74 | 5 | 1.03 | 1.00 |
| vocabulário do corpus | 198 | 2.0 | 101 | hashDivisao | 85 | 15.8% | 113 | 6 | 1.39 | 1.40 |
| vocabulário do corpus | 198 | 2.0 | 101 | hashMultiplicativo | 91 | 9.9% | 107 | 6 | 1.36 | 1.40 |
| vocabulário do corpus | 198 | 4.0 | 53 | hashDivisao | 49 | 7.5% | 149 | 9 | 1.92 | 1.93 |
| vocabulário do corpus | 198 | 4.0 | 53 | hashMultiplicativo | 53 | 0.0% | 145 | 9 | 1.89 | 1.93 |
| palavras sintéticas | 20000 | 0.5 | 40009 | hashDivisao | 15768 | 60.6% | 4232 | 6 | 0.70 | 0.71 |
| palavras sintéticas | 20000 | 0.5 | 40009 | hashMultiplicativo | 15706 | 60.7% | 4294 | 5 | 0.71 | 0.71 |
| palavras sintéticas | 20000 | 1.0 | 20011 | hashDivisao | 12589 | 37.1% | 7411 | 7 | 1.01 | 1.00 |
| palavras sintéticas | 20000 | 1.0 | 20011 | hashMultiplicativo | 12645 | 36.8% | 7355 | 6 | 1.00 | 1.00 |
| palavras sintéticas | 20000 | 2.0 | 10007 | hashDivisao | 8621 | 13.9% | 11379 | 9 | 1.41 | 1.41 |
| palavras sintéticas | 20000 | 2.0 | 10007 | hashMultiplicativo | 8592 | 14.1% | 11408 | 9 | 1.43 | 1.41 |
| palavras sintéticas | 20000 | 4.0 | 5003 | hashDivisao | 4905 | 2.0% | 15095 | 15 | 2.01 | 2.00 |
| palavras sintéticas | 20000 | 4.0 | 5003 | hashMultiplicativo | 4893 | 2.2% | 15107 | 13 | 2.01 | 2.00 |
| termo0..termo19999 | 20000 | 0.5 | 40009 | hashDivisao | 10829 | 72.9% | 9171 | 4 | 0.95 | 0.71 |
| termo0..termo19999 | 20000 | 0.5 | 40009 | hashMultiplicativo | 16131 | 59.7% | 3869 | 4 | 0.68 | 0.71 |
| termo0..termo19999 | 20000 | 1.0 | 20011 | hashDivisao | 12036 | 39.9% | 7964 | 5 | 1.02 | 1.00 |
| termo0..termo19999 | 20000 | 1.0 | 20011 | hashMultiplicativo | 13105 | 34.5% | 6895 | 6 | 0.94 | 1.00 |
| termo0..termo19999 | 20000 | 2.0 | 10007 | hashDivisao | 9197 | 8.1% | 10803 | 5 | 0.97 | 1.41 |
| termo0..termo19999 | 20000 | 2.0 | 10007 | hashMultiplicativo | 8941 | 10.7% | 11059 | 7 | 1.27 | 1.41 |
| termo0..termo19999 | 20000 | 4.0 | 5003 | hashDivisao | 5003 | 0.0% | 14997 | 8 | 1.16 | 2.00 |
| termo0..termo19999 | 20000 | 4.0 | 5003 | hashMultiplicativo | 4961 | 0.8% | 15039 | 11 | 1.72 | 2.00 |

### Histograma do tamanho das cadeias (α = 1)

Quantidade de buckets com 0, 1, 2, 3, 4 e 5 ou mais elementos.

| Conjunto | Função | 0 | 1 | 2 | 3 | 4 | 5+ |
|---|---|---|---|---|---|---|---|
| vocabulário do corpus | hashDivisao | 77 | 65 | 42 | 12 | 2 | 1 |
| vocabulário do corpus | hashMultiplicativo | 75 | 72 | 38 | 8 | 4 | 2 |
| palavras sintéticas | hashDivisao | 7422 | 7327 | 3583 | 1290 | 319 | 70 |
| palavras sintéticas | hashMultiplicativo | 7366 | 7347 | 3670 | 1272 | 294 | 62 |
| termo0..termo19999 | hashDivisao | 7975 | 6066 | 4373 | 1232 | 333 | 32 |
| termo0..termo19999 | hashMultiplicativo | 6906 | 7796 | 3970 | 1129 | 175 | 35 |

## Experimento 2: rotações da AVL conforme a ordem de inserção

### 2a) Pares reais do corpus (435 pares)

"Natural" é a ordem em que o `Main` insere (i < j). Em "aleatória" os valores são médias de 20 embaralhamentos.

| Ordem de inserção | Nós | Altura | Altura mínima | Simples esq. | Simples dir. | Dupla esq-dir | Dupla dir-esq | Total (dupla = 1) |
|---|---|---|---|---|---|---|---|---|
| natural (como o Main) | 435 | 10.0 | 9 | 50.0 | 48.0 | 54.0 | 47.0 | 199.0 |
| crescente | 435 | 9.0 | 9 | 426.0 | 0.0 | 0.0 | 0.0 | 426.0 |
| decrescente | 435 | 9.0 | 9 | 0.0 | 426.0 | 0.0 | 0.0 | 426.0 |
| aleatória (média) | 435 | 10.6 | 9 | 50.8 | 48.4 | 50.3 | 48.9 | 198.4 |

### 2b) 10000 similaridades distintas (aleatórias em [0, 1])

| Ordem de inserção | Nós | Altura | Altura mínima | Simples esq. | Simples dir. | Dupla esq-dir | Dupla dir-esq | Total (dupla = 1) |
|---|---|---|---|---|---|---|---|---|
| crescente | 10000 | 14.0 | 14 | 9986.0 | 0.0 | 0.0 | 0.0 | 9986.0 |
| decrescente | 10000 | 14.0 | 14 | 0.0 | 9986.0 | 0.0 | 0.0 | 9986.0 |
| aleatória (média de 20) | 10000 | 16.0 | 14 | 1163.2 | 1167.0 | 1170.0 | 1159.0 | 4659.1 |

## Experimento 3: tempo de execução com volumes crescentes de documentos

Textos sintéticos de 120 a 260 palavras cada (antes de remover stop words). "Processamento" = normalizar e montar as tabelas hash de todos os documentos, em memória, para cada função de hash. "Comparação" = cosseno de todos os n(n−1)/2 pares. "Inserção AVL" = inserir todos os resultados já calculados.

| Documentos | Pares | Processamento hashDivisao (ms) | Processamento hashMultiplicativo (ms) | Comparação (ms) | Inserção AVL (ms) | Nós AVL | Altura AVL |
|---|---|---|---|---|---|---|---|
| 10 | 45 | 1.63 | 0.95 | 0.18 | 0.01 | 45 | 7 |
| 25 | 300 | 2.74 | 2.77 | 1.19 | 0.11 | 300 | 10 |
| 50 | 1225 | 2.66 | 2.39 | 3.34 | 0.46 | 1220 | 12 |
| 100 | 4950 | 4.04 | 4.42 | 12.61 | 0.82 | 4917 | 15 |
| 200 | 19900 | 4.03 | 4.17 | 44.37 | 4.07 | 19602 | 17 |
| 400 | 79800 | 8.70 | 8.24 | 191.97 | 58.32 | 77484 | 20 |

## Experimento 4: AVL contra lista ordenada

Mesmos n resultados (similaridades aleatórias com 6 casas decimais) em três estruturas: a AVL; uma lista ordenada (`ArrayList`) com inserção pela posição correta (busca binária + deslocamento); e, como referência, a lista preenchida de uma vez e ordenada em lote. A busca exata consulta 2000 chaves existentes e devolve todos os pares da chave. As três estruturas encontram o mesmo total de pares (o programa confere).

| n | Inserção AVL (ms) | Inserção em lista ordenada (ms) | Lista + ordenação em lote (ms) | Busca AVL (µs/consulta) | Busca binária na lista (µs/consulta) | Busca linear na lista (µs/consulta) |
|---|---|---|---|---|---|---|
| 1000 | 0.20 | 0.47 | 0.67 | 0.185 | 0.083 | 0.490 |
| 5000 | 1.84 | 0.84 | 1.15 | 0.165 | 0.101 | 2.622 |
| 20000 | 9.41 | 9.45 | 5.97 | 0.124 | 0.130 | 10.902 |
| 50000 | 19.16 | 43.86 | 16.43 | 0.163 | 0.157 | 36.240 |

