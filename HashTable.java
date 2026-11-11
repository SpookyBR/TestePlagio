import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Tabela hash genérica implementada manualmente (sem HashMap).
 *
 * <p><b>Tratamento de colisões:</b> encadeamento separado (separate chaining).
 * Cada posição (bucket) do array guarda uma lista encadeada de nós; chaves que
 * caem no mesmo bucket são ligadas na mesma lista. Escolhi encadeamento porque
 * (1) a remoção é trivial, (2) a tabela degrada de forma suave quando o fator de
 * carga passa de 1 e (3) o tamanho de cada cadeia é uma métrica direta para
 * comparar as duas funções de dispersão no relatório.
 *
 * <p><b>Funções de dispersão (duas, para comparação):</b>
 * <ul>
 *   <li>{@link FuncaoHash#DIVISAO} (hashDivisao): {@code h(k) = k mod m}, com m primo.</li>
 *   <li>{@link FuncaoHash#MULTIPLICATIVO} (hashMultiplicativo): método da multiplicação de
 *       Knuth/Cormen, {@code h(k) = floor(m * frac(k * A))}, com A = (raiz(5) - 1) / 2.</li>
 * </ul>
 * Ambas partem do mesmo inteiro k obtido da chave (polinômio de base 31 sobre os
 * caracteres, em 64 bits), de modo que a comparação isole o efeito da função em si.
 *
 * <p><b>Redimensionamento:</b> quando o fator de carga ultrapassa 0,75 a tabela
 * dobra de tamanho (próximo primo) e reinsere tudo. Pode ser desligado com
 * {@link #setRedimensionamentoAutomatico(boolean)} para experimentos com capacidade fixa.
 *
 * @param <K> tipo da chave (para String usa-se hash próprio; para outros, hashCode())
 * @param <V> tipo do valor
 */
public class HashTable<K, V> {

    /** As duas funções de dispersão disponíveis. O nome é o que aparece na saída do programa. */
    public enum FuncaoHash {
        DIVISAO("hashDivisao"),
        MULTIPLICATIVO("hashMultiplicativo");

        private final String nome;

        FuncaoHash(String nome) {
            this.nome = nome;
        }

        public String getNome() {
            return nome;
        }
    }

    /** Nó da lista encadeada de um bucket. */
    private static class No<K, V> {
        final K chave;
        V valor;
        No<K, V> proximo;

        No(K chave, V valor, No<K, V> proximo) {
            this.chave = chave;
            this.valor = valor;
            this.proximo = proximo;
        }
    }

    private static final int CAPACIDADE_PADRAO = 101;
    private static final double FATOR_DE_CARGA_MAXIMO = 0.75;
    /** Constante A do método da multiplicação: (raiz(5) - 1) / 2, sugerida por Knuth. */
    private static final double A = 0.6180339887498949;

    private No<K, V>[] tabela;
    private int tamanho;
    private final FuncaoHash funcao;
    private boolean redimensionamentoAutomatico = true;

    public HashTable(FuncaoHash funcao) {
        this(CAPACIDADE_PADRAO, funcao);
    }

    /** A capacidade informada é arredondada para o próximo número primo. */
    public HashTable(int capacidadeInicial, FuncaoHash funcao) {
        if (capacidadeInicial < 1) {
            throw new IllegalArgumentException("Capacidade deve ser >= 1");
        }
        if (funcao == null) {
            throw new IllegalArgumentException("Função de dispersão não pode ser nula");
        }
        this.funcao = funcao;
        this.tabela = novaTabela(proximoPrimo(capacidadeInicial));
    }

    // ------------------------------------------------------------------
    // Operações principais
    // ------------------------------------------------------------------

    /**
     * Insere o par (chave, valor). Se a chave já existe, substitui o valor.
     *
     * @return o valor anterior associado à chave, ou null se a chave era nova
     */
    public V put(K chave, V valor) {
        validarChave(chave);

        No<K, V> no = buscarNo(chave);
        if (no != null) {
            V anterior = no.valor;
            no.valor = valor;
            return anterior;
        }

        if (redimensionamentoAutomatico && (tamanho + 1) > FATOR_DE_CARGA_MAXIMO * tabela.length) {
            redimensionar();
        }

        // Colisão: se o bucket já tem nós, o novo entra na cabeça da lista (O(1)).
        int i = indice(chave, tabela.length);
        tabela[i] = new No<>(chave, valor, tabela[i]);
        tamanho++;
        return null;
    }

    /** @return o valor associado à chave, ou null se ela não existe */
    public V get(K chave) {
        validarChave(chave);
        No<K, V> no = buscarNo(chave);
        return no == null ? null : no.valor;
    }

    public boolean containsKey(K chave) {
        validarChave(chave);
        return buscarNo(chave) != null;
    }

    /** @return o valor removido, ou null se a chave não existia */
    public V remove(K chave) {
        validarChave(chave);
        int i = indice(chave, tabela.length);
        No<K, V> anterior = null;
        for (No<K, V> atual = tabela[i]; atual != null; anterior = atual, atual = atual.proximo) {
            if (atual.chave.equals(chave)) {
                if (anterior == null) {
                    tabela[i] = atual.proximo;
                } else {
                    anterior.proximo = atual.proximo;
                }
                tamanho--;
                return atual.valor;
            }
        }
        return null;
    }

    public int size() {
        return tamanho;
    }

    public boolean isEmpty() {
        return tamanho == 0;
    }

    /** Percorre todos os pares (ordem não especificada). */
    public void forEach(BiConsumer<? super K, ? super V> acao) {
        for (No<K, V> cabeca : tabela) {
            for (No<K, V> no = cabeca; no != null; no = no.proximo) {
                acao.accept(no.chave, no.valor);
            }
        }
    }

    /** @return todas as chaves (ordem não especificada) */
    public List<K> chaves() {
        List<K> lista = new ArrayList<>(tamanho);
        forEach((k, v) -> lista.add(k));
        return lista;
    }

    // ------------------------------------------------------------------
    // Funções de dispersão
    // ------------------------------------------------------------------

    /**
     * Converte a chave em um inteiro de 64 bits. Para String usa um polinômio de
     * base 31 (h = h*31 + c, com overflow natural de long), que depende da posição
     * de cada caractere e portanto distingue anagramas ("amor" x "roma").
     */
    private static long chaveParaInteiro(Object chave) {
        if (chave instanceof String s) {
            long h = 7;
            for (int i = 0; i < s.length(); i++) {
                h = h * 31 + s.charAt(i);
            }
            return h;
        }
        return chave.hashCode();
    }

    /** Índice do bucket para a chave, usando a função escolhida na construção. */
    private int indice(K chave, int m) {
        long k = chaveParaInteiro(chave);
        switch (funcao) {
            case DIVISAO:
                return hashDivisao(k, m);
            case MULTIPLICATIVO:
                return hashMultiplicativo(k, m);
            default:
                throw new IllegalStateException("Função desconhecida: " + funcao);
        }
    }

    /**
     * Método da divisão: h(k) = k mod m.
     * Boa quando m é primo e não está perto de uma potência de 2 (evita que só os
     * bits baixos de k decidam o bucket). Usa divisão sem sinal porque k pode ser
     * negativo após o overflow.
     */
    static int hashDivisao(long k, int m) {
        return (int) Long.remainderUnsigned(k, m);
    }

    /**
     * Método da multiplicação: h(k) = floor(m * frac(k * A)).
     * Menos sensível à escolha de m (funciona bem mesmo se m não for primo).
     * Os 64 bits de k são dobrados em 32 (xor das metades) antes da multiplicação
     * para que o produto em double (53 bits de mantissa) preserve a fração.
     */
    static int hashMultiplicativo(long k, int m) {
        long k32 = (k ^ (k >>> 32)) & 0xFFFFFFFFL;
        double produto = k32 * A;
        double fracao = produto - Math.floor(produto);
        int i = (int) (m * fracao);
        return Math.min(i, m - 1); // proteção contra erro de arredondamento
    }

    // ------------------------------------------------------------------
    // Estatísticas de distribuição (usadas na análise experimental)
    // ------------------------------------------------------------------

    public FuncaoHash getFuncao() {
        return funcao;
    }

    /** Nome da função em uso, como deve aparecer no relatório e no resultado.txt. */
    public String getNomeFuncaoHash() {
        return funcao.getNome();
    }

    public int getCapacidade() {
        return tabela.length;
    }

    public double getFatorDeCarga() {
        return (double) tamanho / tabela.length;
    }

    /** @return vetor com o número de elementos em cada bucket (índice = bucket) */
    public int[] tamanhosDosBuckets() {
        int[] tamanhos = new int[tabela.length];
        for (int i = 0; i < tabela.length; i++) {
            for (No<K, V> no = tabela[i]; no != null; no = no.proximo) {
                tamanhos[i]++;
            }
        }
        return tamanhos;
    }

    public int getBucketsOcupados() {
        int ocupados = 0;
        for (No<K, V> cabeca : tabela) {
            if (cabeca != null) {
                ocupados++;
            }
        }
        return ocupados;
    }

    /**
     * Número de colisões: quantas chaves ocupam um bucket que já tinha outra chave,
     * ou seja, {@code size() - bucketsOcupados()}. Definição calculada sobre o estado
     * atual da tabela, portanto comparável entre as duas funções e não afetada
     * pelas reinserções de um redimensionamento.
     */
    public int getColisoes() {
        return tamanho - getBucketsOcupados();
    }

    /** @return tamanho da maior cadeia (pior caso de busca) */
    public int getMaiorCadeia() {
        int maior = 0;
        for (int t : tamanhosDosBuckets()) {
            maior = Math.max(maior, t);
        }
        return maior;
    }

    /** Resumo em uma linha, pronto para colar em tabela/relatório. */
    public String resumoDistribuicao() {
        return String.format("%s: capacidade=%d, elementos=%d, fatorCarga=%.3f, bucketsOcupados=%d, colisoes=%d, maiorCadeia=%d",
                funcao.getNome(), tabela.length, tamanho, getFatorDeCarga(),
                getBucketsOcupados(), getColisoes(), getMaiorCadeia());
    }

    /** Liga/desliga o redimensionamento (desligar serve para comparar as funções com m fixo). */
    public void setRedimensionamentoAutomatico(boolean ativo) {
        this.redimensionamentoAutomatico = ativo;
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private No<K, V> buscarNo(K chave) {
        for (No<K, V> no = tabela[indice(chave, tabela.length)]; no != null; no = no.proximo) {
            if (no.chave.equals(chave)) {
                return no;
            }
        }
        return null;
    }

    /** Dobra a capacidade (próximo primo) e redistribui todos os nós com a mesma função. */
    private void redimensionar() {
        No<K, V>[] antiga = tabela;
        tabela = novaTabela(proximoPrimo(antiga.length * 2));
        for (No<K, V> cabeca : antiga) {
            No<K, V> no = cabeca;
            while (no != null) {
                No<K, V> proximo = no.proximo;
                int i = indice(no.chave, tabela.length);
                no.proximo = tabela[i];
                tabela[i] = no;
                no = proximo;
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <K, V> No<K, V>[] novaTabela(int capacidade) {
        return (No<K, V>[]) new No[capacidade];
    }

    private static void validarChave(Object chave) {
        if (chave == null) {
            throw new IllegalArgumentException("Chave não pode ser nula");
        }
    }

    private static int proximoPrimo(int n) {
        int candidato = Math.max(2, n);
        while (!ehPrimo(candidato)) {
            candidato++;
        }
        return candidato;
    }

    private static boolean ehPrimo(int n) {
        if (n < 2) return false;
        if (n % 2 == 0) return n == 2;
        for (int d = 3; (long) d * d <= n; d += 2) {
            if (n % d == 0) return false;
        }
        return true;
    }
}
