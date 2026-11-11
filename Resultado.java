import java.util.Locale;

/**
 * Resultado de uma comparação entre dois documentos: nomes dos arquivos e a
 * similaridade calculada. É o elemento guardado nas listas dos nós da AVLTree.
 * Imutável.
 */
public class Resultado {

    private final String documentoA;
    private final String documentoB;
    private final double similaridade;

    public Resultado(String documentoA, String documentoB, double similaridade) {
        this.documentoA = documentoA;
        this.documentoB = documentoB;
        this.similaridade = similaridade;
    }

    public String getDocumentoA() {
        return documentoA;
    }

    public String getDocumentoB() {
        return documentoB;
    }

    public double getSimilaridade() {
        return similaridade;
    }

    /** Formato da saída esperada: {@code doc1.txt <-> doc2.txt = 0.82} (ponto decimal, 2 casas). */
    @Override
    public String toString() {
        return String.format(Locale.US, "%s <-> %s = %.2f", documentoA, documentoB, similaridade);
    }
}
