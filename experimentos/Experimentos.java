import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Experimentos para a seção "Análise Experimental" do relatório. Não faz parte do
 * programa principal: serve só para gerar os números e tabelas (em Markdown).
 *
 * <pre>
 *   Uso (a partir da raiz do projeto, com as classes compiladas):
 *     java Experimentos                          -> usa um corpus sintético de 60 documentos
 *     java Experimentos documentos               -> usa os .txt reais da pasta (exps. 1 e 2)
 *     java Experimentos --gerar documentos 30    -> cria 30 textos de teste na pasta
 * </pre>
 *
 * Experimentos:
 * <ol>
 *   <li>Distribuição das chaves nas duas funções de hash (colisões por bucket).</li>
 *   <li>Rotações da AVL conforme a ordem de inserção.</li>
 *   <li>Tempo de execução com volumes crescentes de documentos.</li>
 *   <li>AVL contra lista ordenada (inserção e busca).</li>
 * </ol>
 *
 * Tempos são médias de várias repetições, depois de uma execução de aquecimento da JVM.
 * Os números variam de máquina para máquina; cite no relatório o ambiente (a saída já
 * traz a versão do Java). Tudo é gravado também em experimentos_resultado.md.
 */
public class Experimentos {

    private static final PrintStream TERMINAL =
            new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
    private static final StringBuilder LOG = new StringBuilder();
    private static final String ARQUIVO_SAIDA = "experimentos_resultado.md";
    private static final int REPETICOES = 5;

    /** Evita que o JIT elimine o trabalho medido (resultado "consumido" aqui). */
    private static volatile long sink;

    private static void p(String linha) {
        TERMINAL.println(linha);
        LOG.append(linha).append('\n');
    }

    public static void main(String[] args) throws IOException {
        if (args.length >= 1 && args[0].equals("--gerar")) {
            if (args.length < 3) {
                System.err.println("Uso: java Experimentos --gerar <diretorio> <quantidade>");
                System.exit(1);
            }
            gerarArquivos(Paths.get(args[1]), Integer.parseInt(args[2]));
            return;
        }

        List<Documento> corpus;
        String origem;
        if (args.length >= 1) {
            corpus = carregarDiretorio(Paths.get(args[0]));
            origem = "documentos reais de `" + args[0] + "` (" + corpus.size() + " arquivos)";
        } else {
            corpus = processar(gerarTextos(60, 42), HashTable.FuncaoHash.MULTIPLICATIVO);
            origem = "corpus sintético de 60 documentos (semente 42)";
        }

        p("# Resultados dos experimentos");
        p("");
        p("- Corpus dos experimentos 1 e 2: " + origem);
        p("- Java: " + System.getProperty("java.version") + " | processadores: "
                + Runtime.getRuntime().availableProcessors());
        p("- Tempos: média de " + REPETICOES + " execuções após aquecimento.");
        p("");

        experimentoHash(corpus);
        experimentoRotacoes(corpus);
        experimentoEscala();
        experimentoAvlVsLista();

        Files.write(Paths.get(ARQUIVO_SAIDA), LOG.toString().getBytes(StandardCharsets.UTF_8));
        System.err.println("\nGravado em " + ARQUIVO_SAIDA);
    }

    // ==================================================================
    // Experimento 1: funções de hash
    // ==================================================================

    private static void experimentoHash(List<Documento> corpus) {
        p("## Experimento 1: distribuição das chaves nas duas funções de dispersão");
        p("");
        p("Cada conjunto de palavras é inserido em tabelas de capacidade fixa (sem redimensionar), "
                + "com a mesma capacidade para as duas funções. `α` é o fator de carga alvo (elementos/buckets). "
                + "Colisões = elementos − buckets ocupados. σ é o desvio padrão do tamanho das cadeias; "
                + "σ esperado é √α, o valor de uma dispersão uniforme ideal (quanto mais perto, melhor).");
        p("");

        List<String> vocabCorpus = vocabularioDoCorpus(corpus);
        List<String> vocabSintetico = vocabularioSintetico(20000, 7);
        List<String> sequencial = new ArrayList<>();
        for (int i = 0; i < 20000; i++) sequencial.add("termo" + i);

        String[] nomes = {"vocabulário do corpus", "palavras sintéticas", "termo0..termo19999"};
        List<List<String>> conjuntos = List.of(vocabCorpus, vocabSintetico, sequencial);
        double[] cargas = {0.5, 1, 2, 4};

        p("| Conjunto | Palavras | α alvo | Buckets (m) | Função | Ocupados | Vazios | Colisões | Maior cadeia | σ | σ esperado |");
        p("|---|---|---|---|---|---|---|---|---|---|---|");
        for (int c = 0; c < conjuntos.size(); c++) {
            List<String> palavras = conjuntos.get(c);
            for (double alfa : cargas) {
                int capacidade = Math.max(2, (int) Math.round(palavras.size() / alfa));
                for (HashTable.FuncaoHash f : HashTable.FuncaoHash.values()) {
                    HashTable<String, Integer> h = preencher(palavras, capacidade, f);
                    int[] tam = h.tamanhosDosBuckets();
                    int m = tam.length;
                    double lambda = (double) h.size() / m;
                    double var = 0;
                    for (int t : tam) var += (t - lambda) * (t - lambda);
                    double sigma = Math.sqrt(var / m);
                    p(String.format(Locale.US, "| %s | %d | %.1f | %d | %s | %d | %.1f%% | %d | %d | %.2f | %.2f |",
                            nomes[c], palavras.size(), alfa, m, f.getNome(), h.getBucketsOcupados(),
                            100.0 * (m - h.getBucketsOcupados()) / m, h.getColisoes(),
                            h.getMaiorCadeia(), sigma, Math.sqrt(lambda)));
                }
            }
        }
        p("");

        p("### Histograma do tamanho das cadeias (α = 1)");
        p("");
        p("Quantidade de buckets com 0, 1, 2, 3, 4 e 5 ou mais elementos.");
        p("");
        p("| Conjunto | Função | 0 | 1 | 2 | 3 | 4 | 5+ |");
        p("|---|---|---|---|---|---|---|---|");
        for (int c = 0; c < conjuntos.size(); c++) {
            List<String> palavras = conjuntos.get(c);
            for (HashTable.FuncaoHash f : HashTable.FuncaoHash.values()) {
                HashTable<String, Integer> h = preencher(palavras, palavras.size(), f);
                int[] faixa = new int[6];
                for (int t : h.tamanhosDosBuckets()) faixa[Math.min(t, 5)]++;
                p(String.format("| %s | %s | %d | %d | %d | %d | %d | %d |", nomes[c], f.getNome(),
                        faixa[0], faixa[1], faixa[2], faixa[3], faixa[4], faixa[5]));
            }
        }
        p("");
    }

    private static HashTable<String, Integer> preencher(List<String> palavras, int capacidade,
                                                        HashTable.FuncaoHash f) {
        HashTable<String, Integer> h = new HashTable<>(capacidade, f);
        h.setRedimensionamentoAutomatico(false);
        for (String w : palavras) h.put(w, 1);
        return h;
    }

    /** Palavras distintas de todos os documentos (a deduplicação usa a própria HashTable). */
    private static List<String> vocabularioDoCorpus(List<Documento> corpus) {
        HashTable<String, Boolean> vistas = new HashTable<>(HashTable.FuncaoHash.DIVISAO);
        List<String> vocab = new ArrayList<>();
        for (Documento d : corpus) {
            for (String w : d.getFrequencias().chaves()) {
                if (!vistas.containsKey(w)) {
                    vistas.put(w, Boolean.TRUE);
                    vocab.add(w);
                }
            }
        }
        return vocab;
    }

    private static List<String> vocabularioSintetico(int n, long semente) {
        String[] silabas = {"ba", "be", "bi", "bo", "bu", "ca", "ce", "ci", "co", "cu", "da", "de", "di", "do",
                "du", "fa", "fe", "fi", "fo", "fu", "ga", "ge", "go", "gu", "la", "le", "li", "lo", "lu", "ma",
                "me", "mi", "mo", "mu", "na", "ne", "ni", "no", "nu", "pa", "pe", "pi", "po", "pu", "ra", "re",
                "ri", "ro", "ru", "sa", "se", "si", "so", "su", "ta", "te", "ti", "to", "tu", "va", "ve", "vi",
                "vo", "vu"};
        String[] fins = {"", "", "ção", "mente", "dade", "ar", "er", "ir", "s", "ão"};
        Random rnd = new Random(semente);
        HashTable<String, Boolean> vistas = new HashTable<>(HashTable.FuncaoHash.DIVISAO);
        List<String> palavras = new ArrayList<>();
        while (palavras.size() < n) {
            StringBuilder sb = new StringBuilder();
            int k = 2 + rnd.nextInt(3);
            for (int i = 0; i < k; i++) sb.append(silabas[rnd.nextInt(silabas.length)]);
            sb.append(fins[rnd.nextInt(fins.length)]);
            String w = sb.toString();
            if (!vistas.containsKey(w)) {
                vistas.put(w, Boolean.TRUE);
                palavras.add(w);
            }
        }
        return palavras;
    }

    // ==================================================================
    // Experimento 2: rotações da AVL
    // ==================================================================

    private static void experimentoRotacoes(List<Documento> corpus) {
        p("## Experimento 2: rotações da AVL conforme a ordem de inserção");
        p("");

        // 2a) similaridades reais do corpus
        List<Resultado> natural = new ArrayList<>();
        compararTodos(corpus, natural);
        p("### 2a) Pares reais do corpus (" + natural.size() + " pares)");
        p("");
        p("\"Natural\" é a ordem em que o `Main` insere (i < j). Em \"aleatória\" os valores são médias de 20 embaralhamentos.");
        p("");
        cabecalhoRotacoes();
        linhaRotacoes("natural (como o Main)", natural, 1, 0);
        List<Resultado> cres = new ArrayList<>(natural);
        cres.sort(Comparator.comparingDouble(Resultado::getSimilaridade));
        linhaRotacoes("crescente", cres, 1, 0);
        List<Resultado> decr = new ArrayList<>(cres);
        Collections.reverse(decr);
        linhaRotacoes("decrescente", decr, 1, 0);
        linhaRotacoes("aleatória (média)", natural, 20, 1);
        p("");

        // 2b) volume maior e valores distintos
        int n = 10000;
        Random rnd = new Random(99);
        List<Resultado> distintos = new ArrayList<>();
        for (int i = 0; i < n; i++) distintos.add(new Resultado("a", "b", rnd.nextDouble()));
        p("### 2b) " + n + " similaridades distintas (aleatórias em [0, 1])");
        p("");
        cabecalhoRotacoes();
        List<Resultado> c2 = new ArrayList<>(distintos);
        c2.sort(Comparator.comparingDouble(Resultado::getSimilaridade));
        linhaRotacoes("crescente", c2, 1, 0);
        List<Resultado> d2 = new ArrayList<>(c2);
        Collections.reverse(d2);
        linhaRotacoes("decrescente", d2, 1, 0);
        linhaRotacoes("aleatória (média de 20)", distintos, 20, 2);
        p("");
    }

    private static void cabecalhoRotacoes() {
        p("| Ordem de inserção | Nós | Altura | Altura mínima | Simples esq. | Simples dir. | Dupla esq-dir | Dupla dir-esq | Total (dupla = 1) |");
        p("|---|---|---|---|---|---|---|---|---|");
    }

    /** Insere {@code lista} em árvores novas {@code reps} vezes (embaralhando se semente > 0) e imprime a média. */
    private static void linhaRotacoes(String rotulo, List<Resultado> lista, int reps, long semente) {
        double se = 0, sd = 0, ded = 0, dde = 0, altura = 0;
        int nos = 0;
        for (int r = 0; r < reps; r++) {
            List<Resultado> ordem = new ArrayList<>(lista);
            if (semente > 0) Collections.shuffle(ordem, new Random(semente * 1000 + r));
            AVLTree t = new AVLTree();
            for (Resultado x : ordem) t.inserir(x);
            RegistroRotacoes rot = t.getRotacoesTotais();
            se += rot.getSimplesEsquerda();
            sd += rot.getSimplesDireita();
            ded += rot.getDuplaEsquerdaDireita();
            dde += rot.getDuplaDireitaEsquerda();
            altura += t.altura();
            nos = t.getNumNos();
        }
        int minima = nos == 0 ? 0 : (int) Math.ceil(Math.log(nos + 1) / Math.log(2) - 1e-12);
        p(String.format(Locale.US, "| %s | %d | %.1f | %d | %.1f | %.1f | %.1f | %.1f | %.1f |",
                rotulo, nos, altura / reps, minima, se / reps, sd / reps, ded / reps, dde / reps,
                (se + sd + ded + dde) / reps));
    }

    // ==================================================================
    // Experimento 3: tempo por volume de documentos
    // ==================================================================

    private static void experimentoEscala() {
        p("## Experimento 3: tempo de execução com volumes crescentes de documentos");
        p("");
        p("Textos sintéticos de 120 a 260 palavras cada (antes de remover stop words). "
                + "\"Processamento\" = normalizar e montar as tabelas hash de todos os documentos, em memória, "
                + "para cada função de hash. \"Comparação\" = cosseno de todos os n(n−1)/2 pares. "
                + "\"Inserção AVL\" = inserir todos os resultados já calculados.");
        p("");
        p("| Documentos | Pares | Processamento hashDivisao (ms) | Processamento hashMultiplicativo (ms) | Comparação (ms) | Inserção AVL (ms) | Nós AVL | Altura AVL |");
        p("|---|---|---|---|---|---|---|---|");

        for (int n : new int[]{10, 25, 50, 100, 200, 400}) {
            List<String> textos = gerarTextos(n, 42);
            double tDiv = medirMs(() -> sink += processar(textos, HashTable.FuncaoHash.DIVISAO).size());
            double tMul = medirMs(() -> sink += processar(textos, HashTable.FuncaoHash.MULTIPLICATIVO).size());

            List<Documento> docs = processar(textos, HashTable.FuncaoHash.MULTIPLICATIVO);
            List<Resultado> resultados = new ArrayList<>();
            double tComp = medirMs(() -> {
                resultados.clear();
                sink += compararTodos(docs, resultados);
            });
            AVLTree ultima = new AVLTree();
            for (Resultado r : resultados) ultima.inserir(r);
            double tAvl = medirMs(() -> {
                AVLTree t = new AVLTree();
                for (Resultado r : resultados) t.inserir(r);
                sink += t.altura();
            });
            p(String.format(Locale.US, "| %d | %d | %.2f | %.2f | %.2f | %.2f | %d | %d |",
                    n, n * (n - 1) / 2, tDiv, tMul, tComp, tAvl, ultima.getNumNos(), ultima.altura()));
        }
        p("");
    }

    // ==================================================================
    // Experimento 4: AVL contra lista ordenada
    // ==================================================================

    private static void experimentoAvlVsLista() {
        p("## Experimento 4: AVL contra lista ordenada");
        p("");
        p("Mesmos n resultados (similaridades aleatórias com 6 casas decimais) em três estruturas: "
                + "a AVL; uma lista ordenada (`ArrayList`) com inserção pela posição correta (busca binária + deslocamento); "
                + "e, como referência, a lista preenchida de uma vez e ordenada em lote. "
                + "A busca exata consulta 2000 chaves existentes e devolve todos os pares da chave. "
                + "As três estruturas encontram o mesmo total de pares (o programa confere).");
        p("");
        p("| n | Inserção AVL (ms) | Inserção em lista ordenada (ms) | Lista + ordenação em lote (ms) | Busca AVL (µs/consulta) | Busca binária na lista (µs/consulta) | Busca linear na lista (µs/consulta) |");
        p("|---|---|---|---|---|---|---|");

        final int consultas = 2000;
        for (int n : new int[]{1000, 5000, 20000, 50000}) {
            Random rnd = new Random(1234 + n);
            List<Resultado> base = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                base.add(new Resultado("a", "b", Math.round(rnd.nextDouble() * 1e6) / 1e6));
            }

            double tAvl = medirMs(() -> {
                AVLTree t = new AVLTree();
                for (Resultado r : base) t.inserir(r);
                sink += t.getNumNos();
            });
            double tLista = medirMs(() -> sink += montarListaOrdenada(base).size());
            double tLote = medirMs(() -> {
                List<Resultado> l = new ArrayList<>(base);
                l.sort(Comparator.comparingDouble(Resultado::getSimilaridade));
                sink += l.size();
            });

            AVLTree arvore = new AVLTree();
            for (Resultado r : base) arvore.inserir(r);
            List<Resultado> lista = montarListaOrdenada(base);

            double[] chaves = new double[consultas];
            for (int i = 0; i < consultas; i++) chaves[i] = base.get(rnd.nextInt(n)).getSimilaridade();

            long esperado = 0;
            for (double k : chaves) esperado += arvore.buscar(k).size();
            long binaria = 0, linear = 0;
            for (double k : chaves) {
                binaria += contarBinaria(lista, k);
                linear += contarLinear(lista, k);
            }
            if (binaria != esperado || linear != esperado) {
                throw new IllegalStateException("Estruturas divergem: AVL=" + esperado
                        + ", binária=" + binaria + ", linear=" + linear);
            }

            double bAvl = medirMs(() -> {
                long s = 0;
                for (double k : chaves) s += arvore.buscar(k).size();
                sink += s;
            });
            double bBin = medirMs(() -> {
                long s = 0;
                for (double k : chaves) s += contarBinaria(lista, k);
                sink += s;
            });
            double bLin = medirMs(() -> {
                long s = 0;
                for (double k : chaves) s += contarLinear(lista, k);
                sink += s;
            });

            p(String.format(Locale.US, "| %d | %.2f | %.2f | %.2f | %.3f | %.3f | %.3f |",
                    n, tAvl, tLista, tLote,
                    bAvl * 1000 / consultas, bBin * 1000 / consultas, bLin * 1000 / consultas));
        }
        p("");
    }

    /** Insere um a um mantendo a lista ordenada (posição por busca binária; add(i, x) desloca o resto). */
    private static List<Resultado> montarListaOrdenada(List<Resultado> origem) {
        List<Resultado> lista = new ArrayList<>(origem.size());
        for (Resultado r : origem) {
            int lo = 0, hi = lista.size();
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (lista.get(mid).getSimilaridade() <= r.getSimilaridade()) lo = mid + 1;
                else hi = mid;
            }
            lista.add(lo, r);
        }
        return lista;
    }

    private static int contarBinaria(List<Resultado> lista, double chave) {
        int lo = 0, hi = lista.size();
        while (lo < hi) { // primeiro índice com similaridade >= chave
            int mid = (lo + hi) >>> 1;
            if (lista.get(mid).getSimilaridade() < chave) lo = mid + 1;
            else hi = mid;
        }
        int c = 0;
        while (lo < lista.size() && lista.get(lo).getSimilaridade() == chave) {
            c++;
            lo++;
        }
        return c;
    }

    private static int contarLinear(List<Resultado> lista, double chave) {
        int c = 0;
        for (int i = 0; i < lista.size(); i++) {
            double s = lista.get(i).getSimilaridade();
            if (s == chave) c++;
            else if (s > chave) break; // lista ordenada: não há mais iguais adiante
        }
        return c;
    }

    // ==================================================================
    // Apoio: comparação, medição, corpus
    // ==================================================================

    /** Todos os pares i < j, na ordem do Main; devolve o número de pares. */
    private static int compararTodos(List<Documento> docs, List<Resultado> saida) {
        int pares = 0;
        for (int i = 0; i < docs.size(); i++) {
            for (int j = i + 1; j < docs.size(); j++) {
                double s = ComparadorDeDocumentos.similaridade(docs.get(i), docs.get(j));
                saida.add(new Resultado(docs.get(i).getNome(), docs.get(j).getNome(), s));
                pares++;
            }
        }
        return pares;
    }

    private static List<Documento> processar(List<String> textos, HashTable.FuncaoHash f) {
        List<Documento> docs = new ArrayList<>(textos.size());
        for (int i = 0; i < textos.size(); i++) {
            docs.add(new Documento("doc" + (i + 1) + ".txt", textos.get(i), f));
        }
        return docs;
    }

    /** Média em ms de {@link #REPETICOES} execuções, após uma de aquecimento. */
    private static double medirMs(Runnable tarefa) {
        tarefa.run(); // aquecimento
        long total = 0;
        for (int i = 0; i < REPETICOES; i++) {
            long ini = System.nanoTime();
            tarefa.run();
            total += System.nanoTime() - ini;
        }
        return total / (double) REPETICOES / 1e6;
    }

    private static List<Documento> carregarDiretorio(Path dir) throws IOException {
        List<Path> arquivos;
        try (Stream<Path> s = Files.list(dir)) {
            arquivos = s.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        if (arquivos.size() < 2) {
            throw new IllegalArgumentException("São necessários ao menos 2 arquivos .txt em " + dir);
        }
        List<Documento> docs = new ArrayList<>();
        for (Path f : arquivos) docs.add(new Documento(f.toString(), HashTable.FuncaoHash.MULTIPLICATIVO));
        return docs;
    }

    // ------------------------------------------------------------------
    // Gerador de textos de teste
    // ------------------------------------------------------------------

    private static final String[][] TOPICOS = {
        // tecnologia
        {"algoritmo", "árvore", "balanceamento", "complexidade", "hash", "tabela", "busca", "ordenação", "memória",
         "processador", "compilador", "variável", "função", "recursão", "grafo", "pilha", "fila", "lista", "vetor",
         "dados", "estrutura", "código", "programa", "sistema", "rede", "servidor", "banco", "consulta", "índice", "chave"},
        // culinária
        {"receita", "farinha", "açúcar", "forno", "massa", "tempero", "cebola", "alho", "azeite", "molho", "panela",
         "fogão", "sobremesa", "bolo", "chocolate", "leite", "ovo", "manteiga", "sal", "pimenta", "cozinha", "prato",
         "arroz", "feijão", "carne", "frango", "peixe", "legumes", "salada", "sopa"},
        // futebol
        {"futebol", "time", "jogador", "campeonato", "gol", "partida", "treinador", "estádio", "torcida", "bola",
         "zagueiro", "atacante", "goleiro", "técnico", "árbitro", "pênalti", "escanteio", "rodada", "título",
         "vitória", "derrota", "empate", "copa", "seleção", "clube", "temporada", "lance", "defesa", "ataque", "meio"},
        // saúde
        {"saúde", "médico", "hospital", "paciente", "tratamento", "vacina", "sintoma", "doença", "exame", "consulta",
         "remédio", "prevenção", "alimentação", "exercício", "sono", "coração", "pressão", "vírus", "imunidade",
         "clínica", "terapia", "enfermeira", "diagnóstico", "cirurgia", "hábito", "rotina", "descanso", "cuidado",
         "equilíbrio", "vida"},
        // história
        {"império", "guerra", "revolução", "rei", "colônia", "independência", "república", "século", "tratado",
         "batalha", "exército", "monarquia", "escravidão", "imigração", "constituição", "presidente", "governo",
         "política", "sociedade", "economia", "café", "indústria", "cidade", "tropas", "documento", "arquivo",
         "memória", "cultura", "tradição", "legado"},
        // meio ambiente
        {"floresta", "rio", "clima", "chuva", "poluição", "reciclagem", "energia", "solar", "oceano", "animal",
         "espécie", "bioma", "desmatamento", "carbono", "aquecimento", "água", "solo", "semente", "planta", "ecossistema",
         "preservação", "sustentável", "lixo", "atmosfera", "montanha", "vento", "terra", "natureza", "verde", "fauna"}
    };

    private static final String[] COMUNS = {"projeto", "trabalho", "pessoas", "tempo", "mundo", "forma", "parte",
        "caso", "ano", "dia", "grupo", "problema", "resultado", "exemplo", "importante", "novo", "grande",
        "melhor", "hoje", "ideia"};

    private static final String[] FILLERS = {"de", "a", "o", "em", "para", "com", "que", "e", "da", "do", "um",
        "uma", "é", "não", "por", "os", "as", "no", "na"};

    /**
     * Gera {@code n} textos em português simples: cada um tem um tema principal (rodízio), algumas
     * palavras de outro tema e palavras comuns, além de stop words, maiúsculas e pontuação para exercitar
     * a normalização. Um em cada seis (a partir do sétimo) é uma cópia alterada de um texto anterior,
     * para produzir pares de alta similaridade (simulando plágio).
     */
    static List<String> gerarTextos(int n, long semente) {
        Random rnd = new Random(semente);
        int temas = TOPICOS.length;
        List<List<String>> tokensDosDocs = new ArrayList<>();
        List<String> textos = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            List<String> tokens = new ArrayList<>();
            if (i >= 6 && i % 6 == 5) {
                // cópia alterada de um documento anterior
                List<String> origem = tokensDosDocs.get(rnd.nextInt(i));
                double troca = 0.05 + rnd.nextDouble() * 0.45;
                String[] pool = TOPICOS[rnd.nextInt(temas)];
                for (String t : origem) {
                    tokens.add(rnd.nextDouble() < troca ? pool[rnd.nextInt(pool.length)] : t);
                }
            } else {
                int tema = i % temas;
                int outro = (tema + 1 + rnd.nextInt(temas - 1)) % temas;
                int tamanho = 120 + rnd.nextInt(140);
                for (int k = 0; k < tamanho; k++) {
                    if (rnd.nextDouble() < 0.35) {
                        tokens.add(FILLERS[rnd.nextInt(FILLERS.length)]);
                    } else {
                        double u = rnd.nextDouble();
                        String[] pool = u < 0.70 ? TOPICOS[tema] : (u < 0.90 ? TOPICOS[outro] : COMUNS);
                        tokens.add(pool[rnd.nextInt(pool.length)]);
                    }
                }
            }
            tokensDosDocs.add(tokens);
            textos.add(renderizar(tokens, rnd));
        }
        return textos;
    }

    /** Junta os tokens em frases com maiúscula inicial, vírgulas e pontos. */
    private static String renderizar(List<String> tokens, Random rnd) {
        StringBuilder sb = new StringBuilder();
        int limite = 8 + rnd.nextInt(7);
        int cont = 0;
        boolean inicio = true;
        for (String t : tokens) {
            sb.append(inicio ? Character.toUpperCase(t.charAt(0)) + t.substring(1) : t);
            cont++;
            if (cont >= limite) {
                sb.append(". ");
                cont = 0;
                limite = 8 + rnd.nextInt(7);
                inicio = true;
            } else {
                sb.append(rnd.nextInt(10) == 0 ? ", " : " ");
                inicio = false;
            }
        }
        return sb.toString().trim() + ".";
    }

    private static void gerarArquivos(Path dir, int n) throws IOException {
        Files.createDirectories(dir);
        List<String> textos = gerarTextos(n, 42);
        for (int i = 0; i < n; i++) {
            Files.write(dir.resolve("doc" + (i + 1) + ".txt"), textos.get(i).getBytes(StandardCharsets.UTF_8));
        }
        System.err.println(n + " textos gerados em " + dir);
    }
}
