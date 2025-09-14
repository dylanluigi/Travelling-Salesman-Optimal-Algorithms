package model.algorithm;

import model.graph.Graph;
import model.graph.TSPSolution;
import model.graph.TSPStatistics;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resolució exacta del TSP utilitzant el mètode de programació dinàmica de Held–Karp amb bitmasks.
 * 
 * Combina totes les classes auxiliars (combinacions i parelles) en un únic fitxer per facilitar-ne l'ús.
 * Aquest algoritme garanteix la solució òptima utilitzant programació dinàmica amb representació
 * de conjunts mitjançant bitmasks per optimitzar l'eficiència.
 *
 * Complexitat temporal: Θ(n² · 2ⁿ)
 * Complexitat espacial: Θ(n · 2ⁿ)
 * Límit pràctic: ~20 ciutats (≈10M d'entrades a la taula DP)
 * 
 * @author Dylan Canning Garcia
 * @version 1.0
 */
public class HeldKarpTSP implements TSPAlgorithm {
    private ProgressCallback progressCallback;
    private TSPStatistics statistics;
    private static final int MAX_PRACTICAL_SIZE = 20;
    private static final int PARALLEL_THRESHOLD = 12; // Utilitzar execució paral·lela per n >= 12
    private final ForkJoinPool forkJoinPool;
    
    /**
     * Constructor per defecte que crea un pool ForkJoin amb tots els processadors disponibles.
     */
    public HeldKarpTSP() {
        this.forkJoinPool = new ForkJoinPool();
    }
    
    /**
     * Constructor que permet especificar el nivell de paral·lelisme.
     * 
     * @param parallelism nombre de fils a utilitzar per al pool ForkJoin
     */
    public HeldKarpTSP(int parallelism) {
        this.forkJoinPool = new ForkJoinPool(parallelism);
    }

    /**
     * Resol el TSP mitjançant el mètode Held–Karp.
     *
     * @param graph graf sobre el qual es calcula la ruta òptima
     * @param withPruning no utilitzat en aquesta implementació
     * @return solució òptima trobada
     */
    @Override
    public TSPSolution solve(Graph graph, boolean withPruning) {
        return solve(graph, withPruning, CancellationToken.NONE);
    }
    
    @Override
    public TSPSolution solve(Graph graph, boolean withPruning, CancellationToken cancellationToken) {
        long startTime = System.currentTimeMillis();
        long startMem  = getUsedMemory();

        int n = graph.getSize();
        if (n > MAX_PRACTICAL_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Held–Karp DP not practical for %d cities (max %d)", n, MAX_PRACTICAL_SIZE)
            );
        }

        // Utilitzar matriu de doubles directament
        double[][] matrix = graph.getAdjacencyMatrix();

        // Executar Held–Karp (paral·lel per a instàncies més grans)
        HeldKarpSolver solver;
        List<Integer> tour;
        
        try {
            if (n >= PARALLEL_THRESHOLD) {
                solver = new ParallelHeldKarpSolver(matrix, 0, forkJoinPool, cancellationToken);
            } else {
                solver = new HeldKarpSolver(matrix, 0);
            }
            tour = solver.calculateHeldKarp();
        } finally {
            // No tancar el pool aquí ja que es pot reutilitzar
        }

        double cost = solver.getOpt();
        statistics = new TSPStatistics();
        statistics.setExecutionTime(System.currentTimeMillis() - startTime);
        statistics.setMemoryUsed((getUsedMemory() - startMem) / (1024.0*1024.0));
        statistics.setBestCost(cost);
        statistics.setNodesExplored(solver.getDictionaryEntries());

        TSPSolution solution = new TSPSolution(tour, cost);
        solution.setStatistics(statistics);
        return solution;
    }

    @Override
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    @Override
    public String getAlgorithmName() {
        return "Held–Karp (Exact DP)";
    }

    /**
     * Retorna la memòria utilitzada actualment.
     *
     * @return memòria utilitzada en bytes
     */
    private long getUsedMemory() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }
    /**
     * Implementació paral·lela de Held-Karp utilitzant el framework ForkJoin.
     * 
     * Aquesta classe estén el solver seqüencial per proporcionar execució paral·lela
     * mitjançant tasques ForkJoin i estructures de dades concurrents.
     */
    private static class ParallelHeldKarpSolver extends HeldKarpSolver {
        private final ForkJoinPool forkJoinPool;
        private final CancellationToken cancellationToken;
        private final ConcurrentHashMap<Pair, DoublePair> concurrentDict;
        
        public ParallelHeldKarpSolver(double[][] matrix, int startingCity, ForkJoinPool pool, CancellationToken token) {
            super(matrix, startingCity);
            this.forkJoinPool = pool;
            this.cancellationToken = token;
            this.concurrentDict = new ConcurrentHashMap<>();
        }
        
        @Override
        public List<Integer> calculateHeldKarp() {
            int size = matrix.length;

            for (int i = 1; i < size; i++) {
                concurrentDict.put(new Pair(1<<i, i), new DoublePair(matrix[0][i], 0));
            }

            for (int subsetSize = 2; subsetSize < size; subsetSize++) {
                final int currentSize = subsetSize;

                Combinations combs = new Combinations(size-1, subsetSize);
                List<int[]> allCombinations = new ArrayList<>();
                while (combs.hasMore()) {
                    allCombinations.add(combs.getNext());
                }

                forkJoinPool.submit(() -> 
                    allCombinations.parallelStream().forEach(subset -> {
                        cancellationToken.throwIfCancellationRequested();
                        processSubset(subset, size);
                    })
                ).join();
            }

            int allBits = (1<<size) - 2;
            DoublePair bestReturn = maxPair;
            List<DoublePair> finals = new ArrayList<>();
            for (int i = 1; i < size; i++) {
                DoublePair entry = concurrentDict.get(new Pair(allBits, i));
                if (entry != null) {
                    finals.add(new DoublePair(entry.first + matrix[i][0], i));
                }
            }
            
            for (DoublePair p: finals) {
                if (p.first < bestReturn.first
                        || (p.first==bestReturn.first && p.second < bestReturn.second)) {
                    bestReturn = p;
                }
            }
            this.opt = bestReturn.first;
            int parent = bestReturn.second;

            List<Integer> path = new ArrayList<>();
            int bits = allBits;
            for (int i = 0; i < size-1; i++) {
                path.add(parent);
                int newBits = bits & ~(1<<parent);
                DoublePair parentEntry = concurrentDict.get(new Pair(bits, parent));
                parent = parentEntry != null ? parentEntry.second : 0;
                bits = newBits;
            }
            
            path.add(0);
            while (path.get(0) != startingCity) {
                path.add(path.remove(0));
            }
            path.add(startingCity);
            
            List<Integer> result = new ArrayList<>();
            for (int i = path.size()-1; i>=0; i--) result.add(path.get(i));

            this.dictionaryEntries = concurrentDict.size();
            return result;
        }
        
        private void processSubset(int[] subset, int size) {
            // Build bitmask
            int bits = 0;
            for (int v: subset) bits |= 1<<v;
            
            // For each destination j in subset, find best predecessor
            for (int j: subset) {
                int prevBits = bits & ~(1<<j);
                DoublePair best = maxPair;
                for (int k: subset) {
                    if (k == j) continue;
                    DoublePair prevEntry = concurrentDict.get(new Pair(prevBits, k));
                    if (prevEntry != null) {
                        double cost = prevEntry.first + matrix[k][j];
                        if (cost < best.first || (cost==best.first && prevEntry.second<best.second)) {
                            best = new DoublePair(cost, k);
                        }
                    }
                }
                concurrentDict.put(new Pair(bits, j), best);
            }
        }
    }
    
    /**
     * Retorna les estadístiques de l'execució.
     *
     * @return estadístiques de l'execució o null si no s'han establert
     */
    private static class HeldKarpSolver {
        protected final double[][] matrix;
        protected final int startingCity;
        protected static final int MAX_INT = Integer.MAX_VALUE;
        protected static final DoublePair maxPair = new DoublePair(Double.MAX_VALUE, MAX_INT);
        protected double opt;
        protected int dictionaryEntries;

        public HeldKarpSolver(double[][] matrix, int startingCity) {
            this.matrix = matrix;
            this.startingCity = startingCity;
        }

        public List<Integer> calculateHeldKarp() {
            int size = matrix.length;
            // Preassignar la mida del mapa per evitar redimensionaments: (n-1)*2^(n-1) entrades
            Map<Pair,DoublePair> dict = new HashMap<>(getEntriesNum(size-1)+1, 1.0f);

            // Casos base: subconjunts de mida 1 (només una ciutat diferent de 0)
            for (int i = 1; i < size; i++) {
                dict.put(new Pair(1<<i, i),
                        new DoublePair(matrix[0][i], 0));
            }

            // Construir per a subconjunts de mida creixent
            for (int subsetSize = 2; subsetSize < size; subsetSize++) {
                Combinations combs = new Combinations(size-1, subsetSize);
                while (combs.hasMore()) {
                    int[] subset = combs.getNext();  // valors dins [1..size-1]
                    // Construir la màscara de bits
                    int bits = 0;
                    for (int v: subset) bits |= 1<<v;

                    // Per a cada destí j del subconjunt, trobar el millor precedent
                    for (int j: subset) {
                        int prevBits = bits & ~(1<<j);
                        // Provar tots els k diferents de j al subconjunt
                        DoublePair best = maxPair;
                        for (int k: subset) {
                            if (k == j) continue;
                            DoublePair prevEntry = dict.get(new Pair(prevBits, k));
                            double cost = prevEntry.first + matrix[k][j];
                            // Desempat pel valor de la ciutat
                            if (cost < best.first || (cost==best.first && prevEntry.second<best.second)) {
                                best = new DoublePair(cost, k);
                            }
                        }
                        dict.put(new Pair(bits, j), best);
                    }
                }
            }

            // Tancar el recorregut: tornar a la ciutat 0
            int allBits = (1<<size) - 2; // bits for cities 1..size-1
            DoublePair bestReturn = maxPair;
            List<DoublePair> finals = new ArrayList<>();
            for (int i = 1; i < size; i++) {
                DoublePair entry = dict.get(new Pair(allBits, i));
                finals.add(new DoublePair(entry.first + matrix[i][0], i));
            }
            // Escollir el millor
            for (DoublePair p: finals) {
                if (p.first < bestReturn.first
                        || (p.first==bestReturn.first && p.second < bestReturn.second)) {
                    bestReturn = p;
                }
            }
            this.opt = bestReturn.first;
            int parent = bestReturn.second;

            // Reconstrucció inversa del camí
            List<Integer> path = new ArrayList<>();
            int bits = allBits;
            for (int i = 0; i < size-1; i++) {
                path.add(parent);
                int newBits = bits & ~(1<<parent);
                parent = dict.get(new Pair(bits, parent)).second;
                bits = newBits;
            }
            path.add(0);
            while (path.get(0) != startingCity) {
                path.add(path.remove(0));
            }
            path.add(startingCity);
            List<Integer> result = new ArrayList<>();
            for (int i = path.size()-1; i>=0; i--) result.add(path.get(i));

            this.dictionaryEntries = dict.size();
            return result;
        }

        public double getOpt() {
            return opt;
        }
        public int getDictionaryEntries() {
            return dictionaryEntries;
        }

        private static int getEntriesNum(int cities) {
            return cities * intPow(2, cities-1);
        }
        private static int intPow(int a, int b) {
            int res = 1;
            while (b > 0) {
                if ((b & 1) == 1) res *= a;
                b >>= 1;
                a *= a;
            }
            return res;
        }
    }

    private static class Combinations {
        private final int n, r, total;
        private final int[] a;
        private int numLeft;

        public Combinations(int n, int r) {
            if (r>n || n<1) throw new IllegalArgumentException();
            this.n = n;
            this.r = r;
            this.a = new int[r];
            for (int i = 0; i < r; i++) a[i] = i+1;
            this.total = computeTotal();
            this.numLeft = total;
        }

        private int computeTotal() {
            BigInteger nf = factorial(n);
            BigInteger rf = factorial(r);
            BigInteger nmrf = factorial(n-r);
            return nf.divide(rf.multiply(nmrf)).intValue();
        }

        public static BigInteger factorial(int x) {
            BigInteger f = BigInteger.ONE;
            for (int i = x; i > 1; i--) f = f.multiply(BigInteger.valueOf(i));
            return f;
        }

        public boolean hasMore() {
            return numLeft > 0;
        }

        public int[] getNext() {
            if (numLeft == total) {
                numLeft--;
                return Arrays.copyOf(a, r);
            }
            int i = r-1;
            while (a[i] == n-r + i + 1) i--;
            a[i]++;
            for (int j = i+1; j<r; j++) {
                a[j] = a[i] + j - i;
            }
            numLeft--;
            return Arrays.copyOf(a, r);
        }
    }

    private static class Pair {
        final int first, second;
        Pair(int f, int s) { first=f; second=s; }
        @Override public int hashCode() { return 31*first + second; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Pair)) return false;
            Pair p = (Pair)o;
            return p.first==first && p.second==second;
        }
    }

    private static class DoublePair {
        final double first;
        final int second;
        DoublePair(double f, int s) { first=f; second=s; }
        @Override public int hashCode() { return Double.hashCode(first) * 31 + second; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof DoublePair)) return false;
            DoublePair p = (DoublePair)o;
            return Double.compare(p.first, first) == 0 && p.second == second;
        }
    }
}
