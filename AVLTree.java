import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Árvore AVL implementada manualmente. A chave de cada nó é a similaridade (double)
 * e o valor é uma <b>lista de {@link Resultado}</b>: vários pares de documentos
 * podem ter exatamente a mesma similaridade, e todos ficam no mesmo nó.
 *
 * <p><b>Balanceamento:</b> altura de null = 0 e de folha = 1. O fator de balanceamento é
 * {@code altura(esq) - altura(dir)}; após cada inserção, se ele sair de [-1, 1] o nó é
 * rebalanceado com um dos quatro casos:
 * <pre>
 *   fb &gt;  1 e fb(esq) &gt;= 0  -> caso Esquerda-Esquerda : rotação simples à direita
 *   fb &gt;  1 e fb(esq) &lt;  0  -> caso Esquerda-Direita   : rotação dupla esquerda-direita
 *   fb &lt; -1 e fb(dir) &lt;= 0  -> caso Direita-Direita    : rotação simples à esquerda
 *   fb &lt; -1 e fb(dir) &gt;  0  -> caso Direita-Esquerda   : rotação dupla direita-esquerda
 * </pre>
 *
 * <p><b>Contagem de rotações:</b> {@link #inserir(double, Resultado)} devolve um
 * {@link RegistroRotacoes} com as rotações daquela inserção; a árvore também acumula o
 * total em {@link #getRotacoesTotais()}. Inserir uma chave já existente apenas
 * acrescenta o par à lista do nó: a estrutura não muda, logo não há rotações.
 *
 * <p><b>Comparação de doubles:</b> as chaves são arredondadas para 9 casas decimais
 * antes de entrar na árvore. Sem isso, similaridades matematicamente iguais (ex.: 0.1+0.2
 * e 0.3) poderiam diferir por erro de ponto flutuante e cair em nós distintos. O valor
 * original continua guardado no {@link Resultado}.
 */
public class AVLTree {

    private static final double ESCALA = 1e9;

    private static class No {
        final double chave;
        final List<Resultado> resultados = new ArrayList<>();
        int altura = 1;
        No esquerdo;
        No direito;

        No(double chave) {
            this.chave = chave;
        }
    }

    private No raiz;
    private int numNos;
    private int numResultados;
    private final RegistroRotacoes rotacoesTotais = new RegistroRotacoes();

    // ------------------------------------------------------------------
    // Inserção
    // ------------------------------------------------------------------

    /** Insere o resultado usando a própria similaridade como chave. */
    public RegistroRotacoes inserir(Resultado resultado) {
        return inserir(resultado.getSimilaridade(), resultado);
    }

    /**
     * Insere o resultado no nó da chave informada (criando o nó se necessário) e
     * rebalanceia a árvore.
     *
     * @return registro com as rotações realizadas nesta inserção
     */
    public RegistroRotacoes inserir(double similaridade, Resultado resultado) {
        if (Double.isNaN(similaridade) || Double.isInfinite(similaridade)) {
            throw new IllegalArgumentException("Similaridade inválida: " + similaridade);
        }
        if (resultado == null) {
            throw new IllegalArgumentException("Resultado não pode ser nulo");
        }
        RegistroRotacoes registro = new RegistroRotacoes();
        raiz = inserir(raiz, normalizar(similaridade), resultado, registro);
        numResultados++;
        rotacoesTotais.somar(registro);
        return registro;
    }

    private No inserir(No no, double chave, Resultado resultado, RegistroRotacoes registro) {
        if (no == null) {
            No novo = new No(chave);
            novo.resultados.add(resultado);
            numNos++;
            return novo;
        }

        if (chave < no.chave) {
            no.esquerdo = inserir(no.esquerdo, chave, resultado, registro);
        } else if (chave > no.chave) {
            no.direito = inserir(no.direito, chave, resultado, registro);
        } else {
            // Chave repetida: só acrescenta o par à lista do nó. Nada muda na forma da árvore.
            no.resultados.add(resultado);
            return no;
        }

        atualizarAltura(no);
        return balancear(no, registro);
    }

    /** Verifica o fator de balanceamento de {@code no} e aplica a rotação adequada, contando-a. */
    private No balancear(No no, RegistroRotacoes registro) {
        int fb = fatorDeBalanceamento(no);

        if (fb > 1) { // pesada à esquerda
            if (fatorDeBalanceamento(no.esquerdo) >= 0) {
                registro.registrarSimplesDireita();                    // caso E-E
                return rotacaoDireita(no);
            }
            registro.registrarDuplaEsquerdaDireita();                  // caso E-D
            no.esquerdo = rotacaoEsquerda(no.esquerdo);
            return rotacaoDireita(no);
        }

        if (fb < -1) { // pesada à direita
            if (fatorDeBalanceamento(no.direito) <= 0) {
                registro.registrarSimplesEsquerda();                   // caso D-D
                return rotacaoEsquerda(no);
            }
            registro.registrarDuplaDireitaEsquerda();                  // caso D-E
            no.direito = rotacaoDireita(no.direito);
            return rotacaoEsquerda(no);
        }

        return no;
    }

    /**
     * Rotação simples à direita:
     * <pre>
     *       y              x
     *      / \            / \
     *     x   C   ==&gt;   A   y
     *    / \                / \
     *   A   B              B   C
     * </pre>
     */
    private No rotacaoDireita(No y) {
        No x = y.esquerdo;
        y.esquerdo = x.direito;
        x.direito = y;
        atualizarAltura(y); // y ficou abaixo de x: atualizar primeiro
        atualizarAltura(x);
        return x;
    }

    /**
     * Rotação simples à esquerda (espelho da anterior):
     * <pre>
     *     x                  y
     *    / \                / \
     *   A   y      ==&gt;     x   C
     *      / \            / \
     *     B   C          A   B
     * </pre>
     */
    private No rotacaoEsquerda(No x) {
        No y = x.direito;
        x.direito = y.esquerdo;
        y.esquerdo = x;
        atualizarAltura(x);
        atualizarAltura(y);
        return y;
    }

    private static int altura(No no) {
        return no == null ? 0 : no.altura;
    }

    private static void atualizarAltura(No no) {
        no.altura = 1 + Math.max(altura(no.esquerdo), altura(no.direito));
    }

    private static int fatorDeBalanceamento(No no) {
        return no == null ? 0 : altura(no.esquerdo) - altura(no.direito);
    }

    private static double normalizar(double x) {
        return Math.round(x * ESCALA) / ESCALA;
    }

    // ------------------------------------------------------------------
    // Consultas e travessias
    // ------------------------------------------------------------------

    /** @return pares com exatamente essa similaridade (lista vazia se não houver) */
    public List<Resultado> buscar(double similaridade) {
        double chave = normalizar(similaridade);
        No no = raiz;
        while (no != null) {
            if (chave < no.chave) {
                no = no.esquerdo;
            } else if (chave > no.chave) {
                no = no.direito;
            } else {
                return Collections.unmodifiableList(no.resultados);
            }
        }
        return Collections.emptyList();
    }

    /** Todos os pares em ordem crescente de similaridade (travessia em-ordem). */
    public List<Resultado> emOrdemCrescente() {
        List<Resultado> saida = new ArrayList<>(numResultados);
        emOrdemCrescente(raiz, saida);
        return saida;
    }

    private void emOrdemCrescente(No no, List<Resultado> saida) {
        if (no == null) return;
        emOrdemCrescente(no.esquerdo, saida);
        saida.addAll(no.resultados);
        emOrdemCrescente(no.direito, saida);
    }

    /** Todos os pares em ordem decrescente de similaridade (em-ordem invertida). */
    public List<Resultado> emOrdemDecrescente() {
        List<Resultado> saida = new ArrayList<>(numResultados);
        emOrdemDecrescente(raiz, saida);
        return saida;
    }

    private void emOrdemDecrescente(No no, List<Resultado> saida) {
        if (no == null) return;
        emOrdemDecrescente(no.direito, saida);
        saida.addAll(no.resultados);
        emOrdemDecrescente(no.esquerdo, saida);
    }

    /**
     * Pares com similaridade {@code >= limiar}, do mais para o menos semelhante.
     * A subárvore esquerda só é visitada se o nó atual já atinge o limiar, então
     * o custo é O(log n + k), com k = pares devolvidos.
     */
    public List<Resultado> acimaDoLimiar(double limiar) {
        List<Resultado> saida = new ArrayList<>();
        acimaDoLimiar(raiz, normalizar(limiar), saida);
        return saida;
    }

    private void acimaDoLimiar(No no, double limiar, List<Resultado> saida) {
        if (no == null) return;
        acimaDoLimiar(no.direito, limiar, saida);
        if (no.chave >= limiar) {
            saida.addAll(no.resultados);
            acimaDoLimiar(no.esquerdo, limiar, saida); // tudo à esquerda é menor; só vale a pena se este nó passou
        }
    }

    /**
     * Os {@code k} pares mais semelhantes (ou todos, se houver menos de k), do maior para o menor.
     * Para empates no corte, vale a ordem de inserção dentro do nó.
     */
    public List<Resultado> topK(int k) {
        if (k < 0) {
            throw new IllegalArgumentException("k deve ser >= 0");
        }
        List<Resultado> saida = new ArrayList<>(Math.min(k, numResultados));
        topK(raiz, k, saida);
        return saida;
    }

    private void topK(No no, int k, List<Resultado> saida) {
        if (no == null || saida.size() >= k) return;
        topK(no.direito, k, saida);
        for (Resultado r : no.resultados) {
            if (saida.size() >= k) return;
            saida.add(r);
        }
        topK(no.esquerdo, k, saida);
    }

    /** Pares do nó de maior similaridade (lista vazia se a árvore está vazia). */
    public List<Resultado> paresMaiorSimilaridade() {
        if (raiz == null) return Collections.emptyList();
        No no = raiz;
        while (no.direito != null) no = no.direito;
        return Collections.unmodifiableList(no.resultados);
    }

    /** Pares do nó de menor similaridade (lista vazia se a árvore está vazia). */
    public List<Resultado> paresMenorSimilaridade() {
        if (raiz == null) return Collections.emptyList();
        No no = raiz;
        while (no.esquerdo != null) no = no.esquerdo;
        return Collections.unmodifiableList(no.resultados);
    }

    // ------------------------------------------------------------------
    // Métricas e verificação
    // ------------------------------------------------------------------

    public boolean isEmpty() {
        return raiz == null;
    }

    /** Altura da árvore (0 se vazia, 1 se só a raiz). */
    public int altura() {
        return altura(raiz);
    }

    /** Número de nós, isto é, de valores de similaridade distintos. */
    public int getNumNos() {
        return numNos;
    }

    /** Número total de pares armazenados (soma das listas de todos os nós). */
    public int getNumResultados() {
        return numResultados;
    }

    /** Cópia (snapshot) do total acumulado de rotações de todas as inserções feitas. */
    public RegistroRotacoes getRotacoesTotais() {
        return new RegistroRotacoes(rotacoesTotais);
    }

    /**
     * Confere as invariantes: ordem de BST, alturas armazenadas corretas e
     * fator de balanceamento em [-1, 1] em todos os nós. Usado nos testes.
     */
    public boolean invariantesValidas() {
        return verificar(raiz, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY) >= 0;
    }

    /** @return altura da subárvore, ou -1 se alguma invariante for violada */
    private int verificar(No no, double min, double max) {
        if (no == null) return 0;
        if (!(no.chave > min && no.chave < max) || no.resultados.isEmpty()) return -1;
        int he = verificar(no.esquerdo, min, no.chave);
        int hd = verificar(no.direito, no.chave, max);
        if (he < 0 || hd < 0) return -1;
        if (Math.abs(he - hd) > 1) return -1;
        int h = 1 + Math.max(he, hd);
        return h == no.altura ? h : -1;
    }
}
