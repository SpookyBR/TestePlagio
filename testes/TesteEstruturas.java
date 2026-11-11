import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Testes das estruturas HashTable e AVLTree (sem JUnit, para rodar só com o JDK):
 * <pre>
 *   javac *.java &amp;&amp; java TesteEstruturas
 * </pre>
 */
public class TesteEstruturas {

    private static int verificacoes = 0;

    private static void check(boolean condicao, String descricao) {
        verificacoes++;
        if (!condicao) {
            throw new AssertionError("FALHOU: " + descricao);
        }
    }

    private static Resultado r(double s) {
        return new Resultado("a.txt", "b.txt", s);
    }

    public static void main(String[] args) {
        testarRotacoesAVL();
        testarChavesRepetidasAVL();
        testarAVLAleatoria();
        testarHash();
        compararFuncoesHash();
        System.out.println("\nTodos os testes passaram (" + verificacoes + " verificações).");
    }

    // ---------------- AVL ----------------

    private static void testarRotacoesAVL() {
        // Cada sequência de 3 chaves força exatamente um dos 4 casos.
        AVLTree t = new AVLTree();
        t.inserir(r(0.1)); t.inserir(r(0.2));
        RegistroRotacoes reg = t.inserir(r(0.3));
        check(reg.getSimplesEsquerda() == 1 && reg.getTotal() == 1, "crescente -> simples esquerda");

        t = new AVLTree();
        t.inserir(r(0.3)); t.inserir(r(0.2));
        reg = t.inserir(r(0.1));
        check(reg.getSimplesDireita() == 1 && reg.getTotal() == 1, "decrescente -> simples direita");

        t = new AVLTree();
        t.inserir(r(0.3)); t.inserir(r(0.1));
        reg = t.inserir(r(0.2));
        check(reg.getDuplaEsquerdaDireita() == 1 && reg.getTotal() == 1, "3,1,2 -> dupla esquerda-direita");
        check(reg.getTotalElementares() == 2, "dupla = 2 rotações elementares");

        t = new AVLTree();
        t.inserir(r(0.1)); t.inserir(r(0.3));
        reg = t.inserir(r(0.2));
        check(reg.getDuplaDireitaEsquerda() == 1 && reg.getTotal() == 1, "1,3,2 -> dupla direita-esquerda");

        // Após qualquer um dos casos a raiz deve ser 0.2 e a altura 2.
        check(t.altura() == 2 && t.invariantesValidas(), "altura 2 e invariantes após rotação");

        // 7 chaves crescentes: n - log2(n+1) = 4 rotações simples à esquerda; árvore perfeita de altura 3.
        t = new AVLTree();
        for (int i = 1; i <= 7; i++) t.inserir(r(i / 10.0));
        RegistroRotacoes total = t.getRotacoesTotais();
        check(total.getSimplesEsquerda() == 4 && total.getTotal() == 4, "7 crescentes -> 4 simples esquerda");
        check(t.altura() == 3, "7 crescentes -> altura 3");
        check(t.emOrdemCrescente().size() == 7, "7 elementos");
    }

    private static void testarChavesRepetidasAVL() {
        AVLTree t = new AVLTree();
        t.inserir(r(0.5));
        RegistroRotacoes reg1 = t.inserir(new Resultado("x.txt", "y.txt", 0.5));
        RegistroRotacoes reg2 = t.inserir(new Resultado("z.txt", "w.txt", 0.5));
        check(t.getNumNos() == 1 && t.getNumResultados() == 3, "3 pares na mesma chave -> 1 nó");
        check(reg1.getTotal() == 0 && reg2.getTotal() == 0, "chave repetida não gera rotação");
        check(t.buscar(0.5).size() == 3, "buscar devolve os 3 pares");
        check(t.buscar(0.6).isEmpty(), "buscar chave ausente -> vazio");

        // Erro de ponto flutuante: 0.1 + 0.2 != 0.3 em double, mas deve cair no mesmo nó.
        t = new AVLTree();
        t.inserir(r(0.1 + 0.2));
        t.inserir(r(0.3));
        check(t.getNumNos() == 1, "0.1+0.2 e 0.3 no mesmo nó");

        // -0.0 e 0.0 também.
        t = new AVLTree();
        t.inserir(r(0.0));
        t.inserir(r(-0.0));
        check(t.getNumNos() == 1, "0.0 e -0.0 no mesmo nó");

        check(new AVLTree().paresMenorSimilaridade().isEmpty(), "árvore vazia -> menor vazio");
        check(new AVLTree().topK(5).isEmpty(), "árvore vazia -> topK vazio");

        boolean lancou = false;
        try {
            new AVLTree().inserir(Double.NaN, r(0.1));
        } catch (IllegalArgumentException e) {
            lancou = true;
        }
        check(lancou, "NaN rejeitado");
    }

    private static void testarAVLAleatoria() {
        Random rnd = new Random(42);
        int n = 20000;
        AVLTree t = new AVLTree();
        List<Resultado> todos = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            // 500 valores possíveis: força muitas chaves repetidas.
            double s = rnd.nextInt(501) / 500.0;
            Resultado res = new Resultado("d" + i, "d" + (i + 1), s);
            todos.add(res);
            t.inserir(res);
            if (i % 2000 == 0) check(t.invariantesValidas(), "invariantes durante inserção " + i);
        }
        check(t.invariantesValidas(), "invariantes ao final");
        check(t.getNumResultados() == n, "total de resultados");
        check(t.getNumNos() <= 501, "nós <= valores distintos");
        // Limite teórico de altura da AVL: 1.44 * log2(n+2)
        check(t.altura() <= 1.45 * (Math.log(t.getNumNos() + 2) / Math.log(2)), "altura dentro do limite teórico");

        // Ordem crescente/decrescente
        List<Resultado> cres = t.emOrdemCrescente();
        check(cres.size() == n, "em-ordem devolve todos");
        for (int i = 1; i < cres.size(); i++) {
            check(cres.get(i - 1).getSimilaridade() <= cres.get(i).getSimilaridade(), "ordem crescente");
        }
        List<Resultado> decr = t.emOrdemDecrescente();
        for (int i = 1; i < decr.size(); i++) {
            check(decr.get(i - 1).getSimilaridade() >= decr.get(i).getSimilaridade(), "ordem decrescente");
        }

        // Limiar: confere contra força bruta
        for (double limiar : new double[]{0.0, 0.25, 0.75, 0.9, 1.0, 1.1}) {
            int esperado = 0;
            for (Resultado x : todos) if (x.getSimilaridade() >= limiar) esperado++;
            List<Resultado> got = t.acimaDoLimiar(limiar);
            check(got.size() == esperado, "acimaDoLimiar(" + limiar + ") = " + esperado + ", obtido " + got.size());
            for (Resultado x : got) check(x.getSimilaridade() >= limiar, "todo resultado >= limiar");
            for (int i = 1; i < got.size(); i++) {
                check(got.get(i - 1).getSimilaridade() >= got.get(i).getSimilaridade(), "limiar em ordem decrescente");
            }
        }

        // topK: compara os valores de similaridade com a ordenação por força bruta
        List<Resultado> ordenados = new ArrayList<>(todos);
        ordenados.sort(Comparator.comparingDouble(Resultado::getSimilaridade).reversed());
        for (int k : new int[]{0, 1, 5, 100, 5000, n, n + 10}) {
            List<Resultado> top = t.topK(k);
            int esperado = Math.min(k, n);
            check(top.size() == esperado, "topK(" + k + ") tamanho");
            for (int i = 0; i < esperado; i++) {
                check(top.get(i).getSimilaridade() == ordenados.get(i).getSimilaridade(), "topK(" + k + ") posição " + i);
            }
        }

        // Extremos
        double max = ordenados.get(0).getSimilaridade();
        double min = ordenados.get(n - 1).getSimilaridade();
        check(t.paresMaiorSimilaridade().get(0).getSimilaridade() == max, "maior");
        check(t.paresMenorSimilaridade().get(0).getSimilaridade() == min, "menor");

        System.out.println("AVL aleatória: n=" + n + ", nós=" + t.getNumNos() + ", altura=" + t.altura()
                + ", " + t.getRotacoesTotais());
    }

    // ---------------- Hash ----------------

    private static void testarHash() {
        for (HashTable.FuncaoHash f : HashTable.FuncaoHash.values()) {
            HashTable<String, Integer> h = new HashTable<>(5, f);
            check(h.isEmpty() && h.get("x") == null, f + ": vazia");

            check(h.put("dados", 1) == null, f + ": put novo devolve null");
            check(h.put("dados", 2) == 1, f + ": put existente devolve valor anterior");
            check(h.get("dados") == 2 && h.size() == 1, f + ": atualização não duplica");
            check(h.containsKey("dados") && !h.containsKey("outra"), f + ": containsKey");

            // Muitas chaves, partindo de capacidade pequena: força vários redimensionamentos.
            int n = 10000;
            for (int i = 0; i < n; i++) h.put("palavra" + i, i);
            check(h.size() == n + 1, f + ": tamanho após inserções");
            for (int i = 0; i < n; i++) check(h.get("palavra" + i) == i, f + ": get palavra" + i);
            check(h.getFatorDeCarga() <= 0.75 + 1e-9, f + ": fator de carga <= 0.75");
            check(h.chaves().size() == n + 1, f + ": chaves()");

            int soma = 0;
            for (int t : h.tamanhosDosBuckets()) soma += t;
            check(soma == h.size(), f + ": soma dos buckets = size");

            check(h.remove("palavra5") == 5 && h.get("palavra5") == null && h.size() == n, f + ": remove");
            check(h.remove("palavra5") == null, f + ": remove inexistente");

            boolean lancou = false;
            try {
                h.put(null, 1);
            } catch (IllegalArgumentException e) {
                lancou = true;
            }
            check(lancou, f + ": chave nula rejeitada");
        }

        // Capacidade fixa: colisões = elementos - buckets ocupados, e anagramas não colidem sempre
        HashTable<String, Integer> h = new HashTable<>(101, HashTable.FuncaoHash.DIVISAO);
        h.setRedimensionamentoAutomatico(false);
        for (int i = 0; i < 1000; i++) h.put("k" + i, i);
        check(h.getCapacidade() == 101, "capacidade fixa");
        check(h.getColisoes() == 1000 - h.getBucketsOcupados(), "definição de colisões");
        check(h.get("k999") == 999, "get com cadeias longas");

        check("hashDivisao".equals(h.getNomeFuncaoHash()), "nome hashDivisao");
        check("hashMultiplicativo".equals(
                new HashTable<String, Integer>(HashTable.FuncaoHash.MULTIPLICATIVO).getNomeFuncaoHash()),
                "nome hashMultiplicativo");

        // Índices sempre dentro de [0, m)
        Random rnd = new Random(7);
        for (int i = 0; i < 100000; i++) {
            long k = rnd.nextLong();
            int m = 2 + rnd.nextInt(5000);
            int a = HashTable.hashDivisao(k, m);
            int b = HashTable.hashMultiplicativo(k, m);
            check(a >= 0 && a < m && b >= 0 && b < m, "índice dentro do intervalo");
        }
    }

    /** Não é um teste: imprime a comparação das duas funções (matéria-prima para o relatório). */
    private static void compararFuncoesHash() {
        System.out.println("\nComparação das funções de dispersão (capacidade fixa, sem redimensionar):");
        String[][] conjuntos = {
                gerarVocabularioSintetico(5000),
                gerarPalavrasSimilares(5000)
        };
        String[] nomes = {"palavras aleatórias (5000)", "palavras com prefixo comum (5000)"};

        for (int c = 0; c < conjuntos.length; c++) {
            System.out.println("  Conjunto: " + nomes[c]);
            for (HashTable.FuncaoHash f : HashTable.FuncaoHash.values()) {
                HashTable<String, Integer> h = new HashTable<>(1009, f);
                h.setRedimensionamentoAutomatico(false);
                for (String p : conjuntos[c]) h.put(p, 1);
                System.out.println("    " + h.resumoDistribuicao());
            }
        }
    }

    private static String[] gerarVocabularioSintetico(int n) {
        Random rnd = new Random(1);
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        while (set.size() < n) {
            int len = 3 + rnd.nextInt(9);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) sb.append((char) ('a' + rnd.nextInt(26)));
            set.add(sb.toString());
        }
        return set.toArray(new String[0]);
    }

    private static String[] gerarPalavrasSimilares(int n) {
        String[] palavras = new String[n];
        for (int i = 0; i < n; i++) palavras[i] = "similaridade" + i;
        return palavras;
    }
}
