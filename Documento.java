import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Representa um único arquivo de texto e o seu conteúdo já processado.
 *
 * <p>Cada documento tem a sua própria {@link HashTable} que mapeia cada palavra
 * significativa (única) à sua frequência no texto. O processamento
 * (normalização) segue estes passos, na ordem:
 * <ol>
 *   <li>converter todo o texto para minúsculas;</li>
 *   <li>remover pontuação e caracteres não-alfanuméricos (viram separadores);</li>
 *   <li>tokenizar, isto é, separar o texto em palavras;</li>
 *   <li>descartar as <i>stop words</i> (lista pré-definida para o português);</li>
 *   <li>contar a frequência de cada palavra restante na tabela hash.</li>
 * </ol>
 *
 * <p><b>Acentos:</b> são preservados ("ciência" continua "ciência"). Por isso
 * "alfanumérico" é decidido por {@link Character#isLetterOrDigit(char)}, que
 * reconhece letras acentuadas, e não por um intervalo fixo como a-z.
 *
 * <p><b>Norma:</b> a norma euclidiana do vetor de frequências é calculada uma
 * única vez, ao fim do processamento, e guardada. O cosseno de um par de
 * documentos usa essa norma, e sem o cache ela seria recalculada em cada um
 * dos n(n-1)/2 pares.
 */
public class Documento {

    /** Palavras com menos letras que isso são ignoradas (ex.: o "d" de "d'água"). */
    private static final int TAMANHO_MINIMO_TOKEN = 2;

    private static final String[] LISTA_STOP_WORDS = {
        "a", "à", "às", "ao", "aos", "aquela", "aquelas", "aquele", "aqueles", "aquilo",
        "as", "até", "com", "como", "da", "das", "de", "dela", "delas", "dele", "deles",
        "depois", "do", "dos", "e", "é", "ela", "elas", "ele", "eles", "em", "entre",
        "era", "eram", "éramos", "essa", "essas", "esse", "esses", "esta", "está",
        "estamos", "estão", "estar", "estas", "estava", "estavam", "este", "esteja",
        "estejam", "estes", "esteve", "estive", "estivemos", "estou", "eu", "foi",
        "fomos", "for", "foram", "fosse", "fossem", "fui", "há", "haja", "hajam",
        "havia", "hei", "houve", "isso", "isto", "já", "lhe", "lhes", "mais", "mas",
        "me", "mesmo", "meu", "meus", "minha", "minhas", "muito", "na", "não", "nas",
        "nem", "no", "nos", "nós", "nossa", "nossas", "nosso", "nossos", "num", "numa",
        "o", "os", "ou", "para", "pela", "pelas", "pelo", "pelos", "per", "perante",
        "por", "porque", "quais", "qual", "quando", "que", "quem", "se", "sem", "seja",
        "sejam", "sejamos", "seu", "seus", "só", "sob", "sobre", "sou", "sua", "suas",
        "são", "também", "te", "tem", "têm", "tende", "tendo", "tenha", "tenham",
        "tenho", "ter", "teu", "teus", "teve", "tinha", "tinham", "tive", "tivemos",
        "tua", "tuas", "tudo", "um", "uma", "umas", "uns", "você", "vocês", "vos"
    };

    /**
     * Stop words do português. Ficam numa HashTable própria (e não num HashSet) para
     * manter a regra de não usar coleções prontas de hash; o valor é irrelevante.
     */
    private static final HashTable<String, Boolean> STOP_WORDS = criarStopWords();

    private static HashTable<String, Boolean> criarStopWords() {
        HashTable<String, Boolean> tabela = new HashTable<>(HashTable.FuncaoHash.DIVISAO);
        for (String palavra : LISTA_STOP_WORDS) {
            tabela.put(palavra, Boolean.TRUE);
        }
        return tabela;
    }

    private final String nome;
    private final HashTable<String, Integer> frequencias;
    private int totalTokens;   // ocorrências de palavras significativas (soma das frequências)
    private double norma;      // sqrt(soma das frequências ao quadrado)

    /**
     * Lê o arquivo, normaliza o texto e preenche a tabela de frequências.
     *
     * @param caminho caminho do arquivo .txt (o nome guardado é só o nome do arquivo)
     * @param funcao  função de dispersão da tabela hash deste documento
     * @throws IOException se o arquivo não puder ser lido
     */
    public Documento(String caminho, HashTable.FuncaoHash funcao) throws IOException {
        this(nomeDoArquivo(caminho), lerArquivo(caminho), funcao);
    }

    /** Cria o documento a partir de um texto já em memória (útil em testes). */
    public Documento(String nome, String texto, HashTable.FuncaoHash funcao) {
        this.nome = nome;
        this.frequencias = new HashTable<>(funcao);
        processar(texto);
    }

    // ------------------------------------------------------------------
    // Leitura
    // ------------------------------------------------------------------

    private static String nomeDoArquivo(String caminho) {
        Path nomeArquivo = Paths.get(caminho).getFileName();
        return nomeArquivo == null ? caminho : nomeArquivo.toString();
    }

    /** Lê o arquivo como UTF-8; bytes inválidos viram U+FFFD (que depois é separador). */
    private static String lerArquivo(String caminho) throws IOException {
        return new String(Files.readAllBytes(Paths.get(caminho)), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // Normalização e contagem
    // ------------------------------------------------------------------

    /** Executa a normalização completa e popula a tabela hash. */
    private void processar(String texto) {
        // NFC: garante que "é" seja um único caractere (e não "e" + acento combinado),
        // senão o acento viraria separador e a palavra seria partida no meio.
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT); // passo 1: minúsculas

        // Passos 2 e 3: percorre o texto e junta letras/dígitos consecutivos num token.
        // Qualquer outro caractere (pontuação, espaço, símbolo) encerra o token atual.
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < normalizado.length(); i++) {
            char c = normalizado.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                token.append(c);
            } else {
                registrarToken(token);
            }
        }
        registrarToken(token); // último token, caso o texto não termine em separador

        calcularNorma();
    }

    /** Aplica o filtro de stop words e conta o token; depois esvazia o buffer. */
    private void registrarToken(StringBuilder token) {
        if (token.length() == 0) {
            return;
        }
        String palavra = token.toString();
        token.setLength(0);

        // Passo 4: descarta tokens muito curtos e stop words.
        if (palavra.length() < TAMANHO_MINIMO_TOKEN || STOP_WORDS.containsKey(palavra)) {
            return;
        }

        // Passo 5: incrementa a frequência (1 se a palavra ainda não está na tabela).
        Integer atual = frequencias.get(palavra);
        frequencias.put(palavra, atual == null ? 1 : atual + 1);
        totalTokens++;
    }

    private void calcularNorma() {
        double[] somaQuadrados = {0.0};
        frequencias.forEach((palavra, freq) -> somaQuadrados[0] += freq.doubleValue() * freq);
        norma = Math.sqrt(somaQuadrados[0]);
    }

    // ------------------------------------------------------------------
    // Acesso
    // ------------------------------------------------------------------

    /** Nome do arquivo (ex.: {@code doc1.txt}), sem o diretório. */
    public String getNome() {
        return nome;
    }

    /** Tabela palavra -> frequência deste documento. */
    public HashTable<String, Integer> getFrequencias() {
        return frequencias;
    }

    /** Frequência da palavra no documento (0 se não aparece). */
    public int getFrequencia(String palavra) {
        Integer f = frequencias.get(palavra);
        return f == null ? 0 : f;
    }

    /** Número de palavras distintas (tamanho do vocabulário do documento). */
    public int getTotalPalavrasDistintas() {
        return frequencias.size();
    }

    /** Número total de ocorrências de palavras significativas. */
    public int getTotalTokens() {
        return totalTokens;
    }

    /** Norma euclidiana do vetor de frequências (0 se o documento ficou vazio). */
    public double getNorma() {
        return norma;
    }

    @Override
    public String toString() {
        return nome + " " + frequencias.size() + " palavras distintas, " + totalTokens + " ocorrências";
    }
}
