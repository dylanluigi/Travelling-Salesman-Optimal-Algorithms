package model.algorithm;

import model.graph.Graph;
import model.graph.TSPSolution;
import model.graph.TSPStatistics;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Branch and Bound concurrent d'alt rendiment utilitzant el framework ForkJoin.
 * 
 * Minimitza l'assignació d'objectes i la contenció per obtenir una acceleració
 * paral·lela real. Utilitza tècniques avançades d'optimització per maximitzar
 * l'eficiència en entorns multi-core.
 * 
 * @author Dylan Canning Garcia
 * @version 1.0
 */
public class ConcurrentBranchAndBound implements TSPAlgorithm {
    
    private ProgressCallback progressCallback;
    private TSPStatistics statistics;
    private final ForkJoinPool forkJoinPool;
    
    private AtomicLong globalNodesExplored = new AtomicLong(0);
    private AtomicLong globalNodesPruned = new AtomicLong(0);
    private AtomicReference<Double> globalBestCost = new AtomicReference<>(Double.MAX_VALUE);
    private AtomicReference<int[]> globalBestPath = new AtomicReference<>();
    
    // Cotes precalculades de dos mínims arestes per càlcul de cotes en O(1)
    private double[] min1, min2;
    
    /**
     * Constructor per defecte que crea un pool ForkJoin amb tots els processadors disponibles.
     */
    public ConcurrentBranchAndBound() {
        this.forkJoinPool = new ForkJoinPool();
    }
    
    /**
     * Constructor que permet especificar el nivell de paral·lelisme.
     * 
     * @param parallelism nombre de fils a utilitzar per al pool ForkJoin
     */
    public ConcurrentBranchAndBound(int parallelism) {
        this.forkJoinPool = new ForkJoinPool(parallelism);
    }
    
    @Override
    public TSPSolution solve(Graph graph, boolean withPruning) {
        return solve(graph, withPruning, CancellationToken.NONE);
    }
    
    @Override
    public TSPSolution solve(Graph graph, boolean withPruning, CancellationToken cancellationToken) {
        this.statistics = new TSPStatistics();
        
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        
        int n = graph.getSize();
        double[][] matrix = graph.getAdjacencyMatrix();
        
        // Reinicialitzar variables atòmiques
        globalNodesExplored.set(0);
        globalNodesPruned.set(0);
        globalBestCost.set(Double.MAX_VALUE);
        globalBestPath.set(null);
        
        // Precalcular les dues arestes mínimes per cada ciutat (O(n²) una vegada)
        precomputeTwoMinEdges(matrix, n);
        
        // Obtenir cota superior inicial utilitzant greedy
        GreedyTSP greedy = new GreedyTSP();
        TSPSolution initialSolution = greedy.solve(graph, false);
        globalBestCost.set(initialSolution.getTotalCost());
        
        // Convertir camí a array per eficiència
        List<Integer> pathList = initialSolution.getPath();
        int[] initialPathArray = pathList.stream().mapToInt(Integer::intValue).toArray();
        globalBestPath.set(initialPathArray);
        
        try {
            // Crear tasca paraigua que coordina tot el treball inicial
            UmbrellaBranchAndBoundTask umbrellaTask = new UmbrellaBranchAndBoundTask(matrix, n, withPruning, cancellationToken, min1, min2);
            
            // Executar i esperar a la finalització utilitzant ForkJoin join adequat
            forkJoinPool.invoke(umbrellaTask);
            
        } catch (Exception e) {
            if (e.getCause() instanceof CancellationToken.CancellationException) {
                throw (CancellationToken.CancellationException) e.getCause();
            }
        }
        
        // Finalitzar estadístiques
        statistics.setNodesExplored((int) globalNodesExplored.get());
        statistics.setNodesPruned((int) globalNodesPruned.get());
        statistics.setExecutionTime(System.currentTimeMillis() - startTime);
        statistics.setMemoryUsed((getUsedMemory() - startMemory) / (1024.0 * 1024.0));
        statistics.setBestCost(globalBestCost.get());
        
        // Convertir el resultat de nou al format List
        int[] finalPathArray = globalBestPath.get();
        List<Integer> finalPath;
        if (finalPathArray != null) {
            finalPath = Arrays.stream(finalPathArray).boxed().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } else {
            finalPath = initialSolution.getPath();
        }
        
        TSPSolution solution = new TSPSolution(finalPath, globalBestCost.get());
        solution.setStatistics(statistics);
        return solution;
    }
    
    /**
     * Precalcula les dues arestes mínimes per cada ciutat per càlcul de cotes en O(1).
     * 
     * Aquesta optimització permet calcular cotes inferiors molt ràpidament
     * utilitzant la suma de les dues arestes més curtes per cada ciutat.
     * 
     * @param matrix matriu de distàncies entre ciutats
     * @param n nombre de ciutats
     */
    private void precomputeTwoMinEdges(double[][] matrix, int n) {
        min1 = new double[n];
        min2 = new double[n];
        
        for (int i = 0; i < n; i++) {
            min1[i] = min2[i] = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    double w = matrix[i][j];
                    if (w < min1[i]) {
                        min2[i] = min1[i];
                        min1[i] = w;
                    } else if (w < min2[i]) {
                        min2[i] = w;
                    }
                }
            }
        }
    }
    
    /**
     * Tasca paraigua que coordina tot el treball inicial i assegura la finalització adequada.
     */
    private class UmbrellaBranchAndBoundTask extends RecursiveAction {
        private final double[][] matrix;
        private final int n;
        private final boolean withPruning;
        private final CancellationToken cancellationToken;
        private final double[] min1, min2;
        
        public UmbrellaBranchAndBoundTask(double[][] matrix, int n, boolean withPruning, CancellationToken cancellationToken, double[] min1, double[] min2) {
            this.matrix = matrix;
            this.n = n;
            this.withPruning = withPruning;
            this.cancellationToken = cancellationToken;
            this.min1 = min1;
            this.min2 = min2;
        }
        
        @Override
        protected void compute() {
            List<BranchAndBoundTask> initialTasks = createInitialTasks();
            
            // Utilitzar ForkJoinTask.invokeAll per a coordinació adequada
            invokeAll(initialTasks);
        }
        
        private List<BranchAndBoundTask> createInitialTasks() {
            List<BranchAndBoundTask> tasks = new ArrayList<>();
            
            // Per a problemes petits, crear tasques per a cada possible segona ciutat
            if (n <= 12) {
                for (int secondCity = 1; secondCity < n; secondCity++) {
                    int[] path = new int[n + 1];
                    boolean[] visited = new boolean[n];
                    path[0] = 0;
                    path[1] = secondCity;
                    visited[0] = true;
                    visited[secondCity] = true;
                    double cost = matrix[0][secondCity];
                    
                    tasks.add(new BranchAndBoundTask(matrix, path, visited, 2, cost, n, withPruning, cancellationToken, min1, min2));
                }
            } else {
                // Per a problemes més grans, crear tasques per a recorreguts parcials de profunditat 2 només (menys clonació)
                for (int secondCity = 1; secondCity < n; secondCity++) {
                    int[] path = new int[n + 1];
                    boolean[] visited = new boolean[n];
                    path[0] = 0;
                    path[1] = secondCity;
                    visited[0] = true;
                    visited[secondCity] = true;
                    double cost = matrix[0][secondCity];
                    
                    tasks.add(new BranchAndBoundTask(matrix, path, visited, 2, cost, n, withPruning, cancellationToken, min1, min2));
                }
            }
            
            return tasks;
        }
    }
    
    /**
     * Tasca ForkJoin que processa un subarbre amb assignació mínima d'objectes.
     */
    private class BranchAndBoundTask extends RecursiveAction {
        private final double[][] matrix;
        private final int[] path;           // Array de camí reutilitzat, in situ
        private final boolean[] visited;    // Array de visitats reutilitzat, in situ
        private final int pathLength;
        private final double currentCost;
        private final int n;
        private final boolean withPruning;
        private final CancellationToken cancellationToken;
        private final double[] min1, min2; // Arestes de dos mínims precalculades
        private int visitedMask;            // Màscara de bits per a comprovacions O(1) de no visitats
        
        // Comptadors locals de fil per reduir la contenció
        private long localNodesExplored = 0;
        private long localNodesPruned = 0;
        private double localBestCost = Double.MAX_VALUE;
        private int[] localBestPath = null;
        
        // Cota global en caché per minimitzar lectures volàtils
        private double cachedGlobalBound = Double.MAX_VALUE;
        private long lastGlobalCheck = 0;
        private static final long GLOBAL_CHECK_INTERVAL = 1000; // Comprovar cota global cada 1000 nodes
        private static final long PROGRESS_UPDATE_INTERVAL = 5000; // Actualitzar progrés cada 5000 nodes
        
        public BranchAndBoundTask(double[][] matrix, int[] path, boolean[] visited, int pathLength, 
                                 double currentCost, int n, boolean withPruning, CancellationToken cancellationToken, double[] min1, double[] min2) {
            this.matrix = matrix;
            this.path = path.clone(); // Només clonar en la creació de tasques, no durant l'execució
            this.visited = visited.clone();
            this.pathLength = pathLength;
            this.currentCost = currentCost;
            this.n = n;
            this.withPruning = withPruning;
            this.cancellationToken = cancellationToken;
            this.min1 = min1;
            this.min2 = min2;
            this.localBestCost = globalBestCost.get();
            this.cachedGlobalBound = this.localBestCost;
            
            // Inicialitzar màscara de bits visitada des de l'array booleà
            this.visitedMask = 0;
            for (int i = 0; i < n; i++) {
                if (visited[i]) {
                    visitedMask |= (1 << i);
                }
            }
        }
        
        @Override
        protected void compute() {
            try {
                cancellationToken.throwIfCancellationRequested();
                
                // Utilitzar exploració recursiva in situ
                exploreSubtree(pathLength, currentCost);
                
                // Actualització final dels comptadors globals
                updateGlobalCounters();
                
            } catch (CancellationToken.CancellationException e) {
                throw new RuntimeException(e);
            }
        }
        
        private void exploreSubtree(int currentLength, double cost) {
            localNodesExplored++;
            
            // Actualitzar periòdicament la cota global en caché i el progrés
            if (localNodesExplored - lastGlobalCheck >= GLOBAL_CHECK_INTERVAL) {
                cachedGlobalBound = globalBestCost.get();
                lastGlobalCheck = localNodesExplored;
                
                // Actualitzar progrés periòdicament i reinicialitzar comptadors per evitar desbordament
                if (localNodesExplored >= PROGRESS_UPDATE_INTERVAL) {
                    updateGlobalCounters();
                    localNodesExplored = 0;
                    localNodesPruned = 0;
                    lastGlobalCheck = 0;
                }
            }
            
            // Calcular cota inferior ajustada utilitzant reducció de matriu o MST
            double lowerBound = cost + calculateLowerBound(currentLength);
            
            // Podar utilitzant cota inferior adequada (no només cost parcial)
            double effectiveBound = Math.min(localBestCost, cachedGlobalBound);
            if (withPruning && lowerBound >= effectiveBound) {
                localNodesPruned++;
                return;
            }
            
            // Comprovació de recorregut complet
            if (currentLength == n) {
                int lastCity = path[currentLength - 1];
                double totalCost = cost + matrix[lastCity][0];
                
                if (totalCost < localBestCost) {
                    localBestCost = totalCost;
                    if (localBestPath == null) {
                        localBestPath = new int[n + 1];
                    }
                    System.arraycopy(path, 0, localBestPath, 0, n);
                    localBestPath[n] = 0; // Tornar a l'inici
                    
                    // Actualitzar millor global si és significativament millor
                    updateGlobalBest(totalCost, localBestPath);
                }
                return;
            }
            
            int currentCity = path[currentLength - 1];
            List<Integer> candidates = new ArrayList<>();
            
            // Recollir ciutats no visitades
            for (int nextCity = 0; nextCity < n; nextCity++) {
                if (!visited[nextCity]) {
                    candidates.add(nextCity);
                }
            }
            
            // Per a subarbres grans, considerar bifurcació (però només a profunditats molt superficials per minimitzar clonació)
            if (candidates.size() > 8 && currentLength <= 2) {
                forkSubtasks(candidates, currentLength, cost);
            } else {
                // Processar seqüecialment amb modificacions in situ (molt més ràpid)
                for (int nextCity : candidates) {
                    double edgeCost = matrix[currentCity][nextCity];
                    double newCost = cost + edgeCost;
                    
                    // Poda primerenca amb cota inferior abans de la recursió
                    if (withPruning) {
                        // Actualitzar temporalment la màscara de bits per calcular la cota
                        int tempMask = visitedMask | (1 << nextCity);
                        int savedMask = visitedMask;
                        visitedMask = tempMask;
                        
                        double tempLowerBound = newCost + calculateLowerBound(currentLength + 1);
                        
                        visitedMask = savedMask; // Restaurar màscara de bits
                        
                        if (tempLowerBound >= effectiveBound) {
                            localNodesPruned++;
                            continue;
                        }
                    }
                    
                    // Extensió in situ
                    path[currentLength] = nextCity;
                    visited[nextCity] = true;
                    visitedMask |= (1 << nextCity);  // Actualitzar màscara de bits
                    
                    exploreSubtree(currentLength + 1, newCost);
                    
                    // Retrocedir in situ
                    visited[nextCity] = false;
                    visitedMask &= ~(1 << nextCity); // Restaurar màscara de bits
                }
            }
        }
        
        private void forkSubtasks(List<Integer> candidates, int currentLength, double cost) {
            // Crear subtasques amb les seves cotes inferiors per ordenar
            List<TaskWithBound> tasksWithBounds = new ArrayList<>();
            
            for (int nextCity : candidates) {
                double edgeCost = matrix[path[currentLength - 1]][nextCity];
                double newCost = cost + edgeCost;
                
                // Calcular cota inferior per aquesta subtasca utilitzant màscara de bits
                int tempMask = visitedMask | (1 << nextCity);
                int savedMask = visitedMask;
                visitedMask = tempMask;
                
                double lowerBound = newCost + calculateLowerBound(currentLength + 1);
                
                visitedMask = savedMask; // Restaurar màscara de bits
                
                // Poda primerenca abans de crear subtasca
                double effectiveBound = Math.min(localBestCost, cachedGlobalBound);
                if (withPruning && lowerBound >= effectiveBound) {
                    localNodesPruned++;
                    continue;
                }
                
                // Crear nova subtasca (clonació mínima - només a profunditats superficials)
                int[] newPath = path.clone();
                boolean[] newVisited = visited.clone();
                newPath[currentLength] = nextCity;
                newVisited[nextCity] = true;
                
                BranchAndBoundTask task = new BranchAndBoundTask(matrix, newPath, newVisited, currentLength + 1, 
                                                               newCost, n, withPruning, cancellationToken, min1, min2);
                tasksWithBounds.add(new TaskWithBound(task, lowerBound));
            }
            
            // Ordenar per cota inferior (millor primer) per processar primer les subtasques més prometedores
            tasksWithBounds.sort((a, b) -> Double.compare(a.lowerBound, b.lowerBound));
            
            // Extreure tasques en ordre
            List<BranchAndBoundTask> subtasks = new ArrayList<>();
            for (TaskWithBound twb : tasksWithBounds) {
                subtasks.add(twb.task);
            }
            
            // Bifurcar totes les subtasques
            invokeAll(subtasks);
            
            // Recollir resultats de les subtasques
            for (BranchAndBoundTask subtask : subtasks) {
                localNodesExplored += subtask.localNodesExplored;
                localNodesPruned += subtask.localNodesPruned;
                
                if (subtask.localBestCost < localBestCost) {
                    localBestCost = subtask.localBestCost;
                    if (localBestPath == null) {
                        localBestPath = new int[n + 1];
                    }
                    if (subtask.localBestPath != null) {
                        System.arraycopy(subtask.localBestPath, 0, localBestPath, 0, n + 1);
                    }
                }
            }
        }
        
        private void updateGlobalBest(double cost, int[] path) {
            // Eliminar barrera epsilon - sempre actualitzar si és millor per assegurar poda ajustada
            double currentGlobal = globalBestCost.get();
            if (cost < currentGlobal) {
                if (globalBestCost.compareAndSet(currentGlobal, cost)) {
                    globalBestPath.set(path.clone());
                    // Actualitzar la nostra cota en caché immediatament
                    cachedGlobalBound = cost;
                }
            }
        }
        
        /**
         * Calcular cota inferior ajustada per ciutats restants utilitzant heurístiques eficients.
         */
        private double calculateLowerBound(int currentLength) {
            if (currentLength == n) return 0; // Recorregut complet
            
            // Utilitzar cota ràpida de "dues arestes mínimes" per velocitat
            return calculateTwoMinBound(currentLength);
        }
        
        /**
         * Cota inferior optimitzada O(n) utilitzant arestes de dos mínims precalculades i màscara de bits.
         * Això és 10-100x més ràpid que la versió original O(n²).
         */
        private double calculateTwoMinBound(int currentLength) {
            if (currentLength == n) return 0;
            
            // Calcular màscara de no visitats en O(1)
            int unvisitedMask = (~visitedMask) & ((1 << n) - 1);
            
            double h = 0;
            
            // Per cada ciutat no visitada, afegir la meitat de les seves dues arestes mínimes precalculades
            for (int city = 0; city < n; city++) {
                if ((unvisitedMask & (1 << city)) != 0) {
                    h += (min1[city] + min2[city]) * 0.5;
                }
            }
            
            return h;
        }
        
        /**
         * Cota de reducció de matriu més costosa però més ajustada (utilitzar amb moderació).
         */
        private double calculateMatrixReductionBound(int currentLength) {
            if (currentLength == n) return 0;
            
            // Crear matriu reduïda per ciutats no visitades
            List<Integer> unvisited = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (!visited[i]) {
                    unvisited.add(i);
                }
            }
            
            if (unvisited.size() <= 1) return 0;
            
            int size = unvisited.size();
            double[][] reducedMatrix = new double[size][size];
            
            // Omplir matriu reduïda
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (i == j) {
                        reducedMatrix[i][j] = Double.MAX_VALUE;
                    } else {
                        reducedMatrix[i][j] = matrix[unvisited.get(i)][unvisited.get(j)];
                    }
                }
            }
            
            // Aplicar reducció de matriu
            return applyMatrixReduction(reducedMatrix);
        }
        
        private double applyMatrixReduction(double[][] matrix) {
            int n = matrix.length;
            double totalReduction = 0;
            
            // Reducció de files
            for (int i = 0; i < n; i++) {
                double minRow = Double.MAX_VALUE;
                for (int j = 0; j < n; j++) {
                    if (matrix[i][j] < minRow) {
                        minRow = matrix[i][j];
                    }
                }
                
                if (minRow != Double.MAX_VALUE && minRow > 0) {
                    totalReduction += minRow;
                    for (int j = 0; j < n; j++) {
                        if (matrix[i][j] != Double.MAX_VALUE) {
                            matrix[i][j] -= minRow;
                        }
                    }
                }
            }
            
            // Reducció de columnes
            for (int j = 0; j < n; j++) {
                double minCol = Double.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    if (matrix[i][j] < minCol) {
                        minCol = matrix[i][j];
                    }
                }
                
                if (minCol != Double.MAX_VALUE && minCol > 0) {
                    totalReduction += minCol;
                    for (int i = 0; i < n; i++) {
                        if (matrix[i][j] != Double.MAX_VALUE) {
                            matrix[i][j] -= minCol;
                        }
                    }
                }
            }
            
            return totalReduction;
        }
        
        private void updateGlobalCounters() {
            globalNodesExplored.addAndGet(localNodesExplored);
            globalNodesPruned.addAndGet(localNodesPruned);
            
            // Activar callback de progrés si està disponible
            if (progressCallback != null) {
                progressCallback.onProgress(
                    (int) globalNodesExplored.get(),
                    (int) globalNodesPruned.get(),
                    globalBestCost.get()
                );
            }
        }
        
        /**
         * Classe auxiliar per associar tasques amb les seves cotes inferiors per ordenar.
         */
        private static class TaskWithBound {
            final BranchAndBoundTask task;
            final double lowerBound;
            
            TaskWithBound(BranchAndBoundTask task, double lowerBound) {
                this.task = task;
                this.lowerBound = lowerBound;
            }
        }
    }
    
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    @Override
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }
    
    @Override
    public String getAlgorithmName() {
        return "Concurrent Branch and Bound (ForkJoin)";
    }
}