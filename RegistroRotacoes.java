/**
 * Registro que quantifica as rotações realizadas pela árvore AVL.
 *
 * <p>Convenção de contagem: uma <b>rotação dupla conta como 1 dupla</b> (e não como 2 simples),
 * mesmo que internamente sejam feitas duas rotações elementares. Os quatro casos são
 * contados separadamente:
 * <ul>
 *   <li>simples à esquerda  (caso Direita-Direita)</li>
 *   <li>simples à direita   (caso Esquerda-Esquerda)</li>
 *   <li>dupla esquerda-direita (caso Esquerda-Direita)</li>
 *   <li>dupla direita-esquerda (caso Direita-Esquerda)</li>
 * </ul>
 * {@link #getTotalElementares()} converte para o número de rotações elementares
 * (simples + 2 x duplas), útil para comparar custo real.
 */
public class RegistroRotacoes {

    private int simplesEsquerda;
    private int simplesDireita;
    private int duplaEsquerdaDireita;
    private int duplaDireitaEsquerda;

    public RegistroRotacoes() {
    }

    /** Cópia (snapshot) de outro registro. */
    public RegistroRotacoes(RegistroRotacoes outro) {
        this.simplesEsquerda = outro.simplesEsquerda;
        this.simplesDireita = outro.simplesDireita;
        this.duplaEsquerdaDireita = outro.duplaEsquerdaDireita;
        this.duplaDireitaEsquerda = outro.duplaDireitaEsquerda;
    }

    // Os incrementos são restritos ao pacote: só a AVLTree altera os contadores.
    void registrarSimplesEsquerda() {
        simplesEsquerda++;
    }

    void registrarSimplesDireita() {
        simplesDireita++;
    }

    void registrarDuplaEsquerdaDireita() {
        duplaEsquerdaDireita++;
    }

    void registrarDuplaDireitaEsquerda() {
        duplaDireitaEsquerda++;
    }

    /** Acumula outro registro neste. */
    void somar(RegistroRotacoes outro) {
        simplesEsquerda += outro.simplesEsquerda;
        simplesDireita += outro.simplesDireita;
        duplaEsquerdaDireita += outro.duplaEsquerdaDireita;
        duplaDireitaEsquerda += outro.duplaDireitaEsquerda;
    }

    public int getSimplesEsquerda() {
        return simplesEsquerda;
    }

    public int getSimplesDireita() {
        return simplesDireita;
    }

    public int getDuplaEsquerdaDireita() {
        return duplaEsquerdaDireita;
    }

    public int getDuplaDireitaEsquerda() {
        return duplaDireitaEsquerda;
    }

    public int getTotalSimples() {
        return simplesEsquerda + simplesDireita;
    }

    public int getTotalDuplas() {
        return duplaEsquerdaDireita + duplaDireitaEsquerda;
    }

    /** Rotações contadas pela convenção da classe (dupla = 1). */
    public int getTotal() {
        return getTotalSimples() + getTotalDuplas();
    }

    /** Rotações elementares executadas (dupla = 2 simples). */
    public int getTotalElementares() {
        return getTotalSimples() + 2 * getTotalDuplas();
    }

    @Override
    public String toString() {
        return String.format(
                "Rotacoes[simplesEsq=%d, simplesDir=%d, duplaEsqDir=%d, duplaDirEsq=%d | simples=%d, duplas=%d, total=%d]",
                simplesEsquerda, simplesDireita, duplaEsquerdaDireita, duplaDireitaEsquerda,
                getTotalSimples(), getTotalDuplas(), getTotal());
    }
}
