/*
 * Verificador de Similaridade de Textos
 * Projeto e Análise de Algoritmos II - Universidade Presbiteriana Mackenzie
 *
 * Integrantes do grupo:
 *   - Arthur Candido Palma 1 - RA: 10743580
 *   - Kaue Cordeiro Abreu 2 - RA: 10741435
 *   - Miguel Bastos Dias Barbosa 3 - RA: 10736603
 */

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Main {

    /** Função de dispersão usada nas tabelas hash dos documentos. */
    private static final HashTable.FuncaoHash FUNCAO_HASH = HashTable.FuncaoHash.MULTIPLICATIVO;

    private static final String ARQUIVO_SAIDA = "resultado.txt";
    private static final String TITULO = "=== VERIFICADOR DE SIMILARIDADE DE TEXTOS ===";

    /** terminal em UTF-8 */
    private static final PrintStream TERMINAL =
            new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

    /** texto a ser gravado em resultado.txt. */
    private static final StringBuilder LOG = new StringBuilder();

    /** imprime no terminal e adiciona ao arquivo de log. */
    private static void saida(String linha) {
        TERMINAL.println(linha);
        LOG.append(linha).append(System.lineSeparator());
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            uso("Argumentos insuficientes.");
            return;
        }

        Path diretorio = Paths.get(args[0]);
        double limiar;
        try {
            limiar = Double.parseDouble(args[1].replace(',', '.'));
        } catch (NumberFormatException e) {
            uso("Limiar inválido: " + args[1]);
            return;
        }
        if (Double.isNaN(limiar) || limiar < 0.0 || limiar > 1.0) {
            uso("O limiar deve estar entre 0 e 1.");
            return;
        }
        if (!Files.isDirectory(diretorio)) {
            uso("Diretório não encontrado: " + args[0]);
            return;
        }

        String modo = args[2].toLowerCase(Locale.ROOT);
        try {
            switch (modo) {
                case "lista":
                    executarLista(diretorio, limiar);
                    break;
                case "topk":
                    if (args.length < 4) {
                        uso("O modo topK exige o valor de K.");
                        return;
                    }
                    int k;
                    try {
                        k = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        uso("Valor de K inválido: " + args[3]);
                        return;
                    }
                    if (k <= 0) {
                        uso("K deve ser maior que zero.");
                        return;
                    }
                    executarTopK(diretorio, k);
                    break;
                case "busca":
                    if (args.length < 5) {
                        uso("O modo busca exige dois nomes de arquivo.");
                        return;
                    }
                    executarBusca(diretorio, args[3], args[4]);
                    break;
                default:
                    uso("Modo desconhecido: " + args[2]);
                    return;
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
            System.exit(1);
            return;
        }

        gravarLog();
    }

    
    // modos de execução
    

    private static void executarLista(Path diretorio, double limiar) throws IOException {
        List<Documento> docs = carregarDocumentos(diretorio);
        AVLTree arvore = new AVLTree();
        cabecalho(docs.size(), compararTodos(docs, arvore));

        saida(String.format(Locale.US, "Pares com similaridade >= %.2f:", limiar));
        imprimirLista(arvore.acimaDoLimiar(limiar));

        saida("");
        saida("Pares com menor similaridade:");
        imprimirLista(arvore.paresMenorSimilaridade());

        resumoArvore(arvore);
    }

    private static void executarTopK(Path diretorio, int k) throws IOException {
        List<Documento> docs = carregarDocumentos(diretorio);
        AVLTree arvore = new AVLTree();
        cabecalho(docs.size(), compararTodos(docs, arvore));

        saida("Os " + k + " pares mais semelhantes:");
        imprimirLista(arvore.topK(k));

        resumoArvore(arvore);
    }

    private static void executarBusca(Path diretorio, String nomeA, String nomeB) throws IOException {
        Documento a = new Documento(resolver(diretorio, nomeA).toString(), FUNCAO_HASH);
        Documento b = new Documento(resolver(diretorio, nomeB).toString(), FUNCAO_HASH);
        double sim = ComparadorDeDocumentos.similaridade(a, b);

        saida(TITULO);
        saida("Comparando: " + nomeA + " <-> " + nomeB);
        saida(String.format(Locale.US, "Similaridade calculada: %.2f", sim));
        saida("Métrica utilizada: " + ComparadorDeDocumentos.NOME_METRICA);
    }

    
    // auxiliares
    

    /** le todos os .txt do diretório . */
    private static List<Documento> carregarDocumentos(Path diretorio) throws IOException {
        List<Path> arquivos;
        try (Stream<Path> s = Files.list(diretorio)) {
            arquivos = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        if (arquivos.size() < 2) {
            throw new IllegalArgumentException(
                    "São necessários ao menos 2 arquivos .txt em " + diretorio + " (encontrados: " + arquivos.size() + ").");
        }
        List<Documento> docs = new ArrayList<>();
        for (Path p : arquivos) {
            docs.add(new Documento(p.toString(), FUNCAO_HASH));
        }
        return docs;
    }

    /**
     * compara todos os pares (i, j) com i &lt; j e insere cada resultado na AVL.
     *
     * @return total de pares comparados, n(n-1)/2.
     */
    private static int compararTodos(List<Documento> docs, AVLTree arvore) {
        int pares = 0;
        for (int i = 0; i < docs.size(); i++) {
            for (int j = i + 1; j < docs.size(); j++) {
                Documento a = docs.get(i);
                Documento b = docs.get(j);
                double sim = ComparadorDeDocumentos.similaridade(a, b);
                arvore.inserir(new Resultado(a.getNome(), b.getNome(), sim));
                pares++;
            }
        }
        return pares;
    }

    private static void cabecalho(int totalDocs, int totalPares) {
        saida(TITULO);
        saida("Total de documentos processados: " + totalDocs);
        saida("Total de pares comparados: " + totalPares);
        saida("Função hash utilizada: " + FUNCAO_HASH.getNome());
        saida("Métrica de similaridade: " + ComparadorDeDocumentos.NOME_METRICA);
        saida("");
    }

    private static void imprimirLista(List<Resultado> lista) {
        if (lista.isEmpty()) {
            saida("(nenhum par)");
            return;
        }
        for (Resultado r : lista) {
            saida(r.toString());
        }
    }

    /** estatísticas da AVL (usadas na análise experimental do relatório). */
    private static void resumoArvore(AVLTree arvore) {
        RegistroRotacoes rot = arvore.getRotacoesTotais();
        saida("");
        saida("AVL: " + arvore.getNumNos() + " nós, altura " + arvore.altura());
        saida("Rotações: " + rot.getTotalSimples() + " simples, " + rot.getTotalDuplas() + " duplas");
    }

    /** aceita "doc1.txt" relativo ao diretório */
    private static Path resolver(Path diretorio, String nome) {
        Path p = diretorio.resolve(nome);
        if (!Files.isRegularFile(p)) {
            throw new IllegalArgumentException("Arquivo não encontrado: " + p);
        }
        return p;
    }

    private static void gravarLog() {
        try {
            Files.write(Paths.get(ARQUIVO_SAIDA), LOG.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Não foi possível gravar " + ARQUIVO_SAIDA + ": " + e.getMessage());
        }
    }

    private static void uso(String erro) {
        System.err.println(erro);
        System.err.println("Uso: java Main <diretorio_documentos> <limiar> <modo> [argumentos_opcionais]");
        System.err.println("  java Main documentos 0.75 lista");
        System.err.println("  java Main documentos 0.8 topk 5");
        System.err.println("  java Main documentos 0.0 busca doc1.txt doc4.txt");
        System.exit(1);
    }
}