/**
 * Isola o cálculo de similaridade entre dois {@link Documento}.
 *
 * <p><b>Métrica: similaridade do cosseno</b> entre os vetores de frequência de
 * termos dos dois documentos (modelo de espaço vetorial):
 * <pre>
 *   cos(A, B) = (A . B) / (|A| * |B|)
 *             = soma(fA(t) * fB(t)) / (sqrt(soma fA(t)^2) * sqrt(soma fB(t)^2))
 * </pre>
 * onde fA(t) é a frequência do termo t no documento A.
 *
 * <p><b>Por que cosseno:</b>
 * <ul>
 *   <li>usa as frequências que já estão nas tabelas hash (Jaccard e Dice só olhariam
 *       presença/ausência e jogariam a contagem fora);</li>
 *   <li>é invariante ao tamanho do texto: um documento copiado e colado duas vezes
 *       continua com similaridade 1 em relação ao original, pois só o ângulo entre os
 *       vetores importa, não o comprimento;</li>
 *   <li>como as frequências nunca são negativas, o resultado fica sempre em [0, 1],
 *       o mesmo intervalo do limiar da linha de comando.</li>
 * </ul>
 *
 * <p><b>Custo:</b> o produto escalar percorre só o vocabulário do documento menor e
 * consulta o outro na tabela hash, portanto O(min(|VA|, |VB|)) buscas de custo médio
 * O(1). As normas já vêm calculadas em {@link Documento#getNorma()}.
 */
public class ComparadorDeDocumentos {

    /** Nome da métrica, como deve aparecer na saída do programa. */
    public static final String NOME_METRICA = "Cosseno";

    private ComparadorDeDocumentos() {
        // classe utilitária: só métodos estáticos
    }

    /**
     * Similaridade do cosseno entre dois documentos.
     *
     * @return valor em [0, 1]; 0 se algum dos documentos não tem nenhuma palavra
     *         significativa (evita a divisão 0/0, que daria NaN)
     */
    public static double similaridade(Documento a, Documento b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Documentos não podem ser nulos");
        }

        double normaA = a.getNorma();
        double normaB = b.getNorma();
        if (normaA == 0.0 || normaB == 0.0) {
            return 0.0; // documento vazio após o pré-processamento
        }

        double cosseno = produtoEscalar(a, b) / (normaA * normaB);

        // Erro de arredondamento pode dar 1.0000000000000002 para textos idênticos.
        return Math.max(0.0, Math.min(1.0, cosseno));
    }

    /**
     * Produto escalar dos vetores de frequência. Itera o documento com menos palavras
     * distintas: termos que só existem no outro contribuem com zero, então não
     * precisam ser visitados.
     */
    private static double produtoEscalar(Documento a, Documento b) {
        Documento menor = a.getTotalPalavrasDistintas() <= b.getTotalPalavrasDistintas() ? a : b;
        Documento maior = (menor == a) ? b : a;

        long[] soma = {0L}; // array de 1 posição: lambdas só podem alterar variáveis efetivamente finais
        menor.getFrequencias().forEach((palavra, freqMenor) -> {
            int freqMaior = maior.getFrequencia(palavra);
            if (freqMaior > 0) {
                soma[0] += (long) freqMenor * freqMaior;
            }
        });
        return soma[0];
    }
}
