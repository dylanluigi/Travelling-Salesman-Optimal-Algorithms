package model.algorithm;

import model.graph.Graph;
import model.graph.TSPSolution;
import model.graph.TSPStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * Implementació de l'algorisme Greedy per al problema del viatjant de comerç.
 * 
 * Aquest algorisme heurístic proporciona una solució ràpida però no necessàriament
 * òptima, seleccionant sempre l'aresta més curta disponible en cada pas.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class GreedyTSP implements TSPAlgorithm {
    
    private ProgressCallback progressCallback;
    private final int numThreads;
    
    /**
     * Constructor per defecte que utilitza tots els processadors disponibles.
     */
    public GreedyTSP() {
        this.numThreads = Runtime.getRuntime().availableProcessors();
    }
    
    /**
     * Constructor que permet especificar el nombre de fils a utilitzar.
     * 
     * @param numThreads nombre de fils per a l'execució paral·lela
     */
    public GreedyTSP(int numThreads) {
        this.numThreads = numThreads;
    }
    
    @Override
    public TSPSolution solve(Graph graph, boolean withPruning) {
        return solve(graph, withPruning, CancellationToken.NONE);
    }
    
    @Override
    public TSPSolution solve(Graph graph, boolean withPruning, CancellationToken cancellationToken) {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        
        int n = graph.getSize();
        double[][] matrix = graph.getAdjacencyMatrix();
        
        TSPSolution solution;
        
        // Utilitzar execució paral·lela per a instàncies més grans
        if (n >= 10 && numThreads > 1) {
            solution = solveParallelGreedy(matrix, n, cancellationToken);
        } else {
            // Un sol fil per a instàncies petites
            solution = solveSimpleNearestNeighbor(matrix, n, 0);
        }
        
        // Crear estadístiques finals
        TSPStatistics statistics = new TSPStatistics();
        statistics.setNodesExplored(n * (n >= 10 ? Math.min(numThreads, n) : 1));
        statistics.setNodesPruned(0);
        statistics.setExecutionTime(System.currentTimeMillis() - startTime);
        statistics.setMemoryUsed((getUsedMemory() - startMemory) / (1024.0 * 1024.0));
        statistics.setBestCost(solution.getTotalCost());
        
        solution.setStatistics(statistics);
        return solution;
    }
    
    
    /**
     * Execució greedy paral·lela amb múltiples reinicis des de diferents ciutats inicials.
     * 
     * @param matrix matriu de distàncies entre ciutats
     * @param n nombre de ciutats
     * @param cancellationToken token per cancel·lar l'execució
     * @return millor solució trobada entre totes les execucions paral·leles
     */
    private TSPSolution solveParallelGreedy(double[][] matrix, int n, CancellationToken cancellationToken) {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        try {
            // Enviar tasques per a diferents ciutats inicials
            List<Future<TSPSolution>> futures = new ArrayList<>();
            int tasksToSubmit = Math.min(numThreads, n);
            
            for (int startCity = 0; startCity < tasksToSubmit; startCity++) {
                final int start = startCity;
                futures.add(executor.submit(() -> {
                    cancellationToken.throwIfCancellationRequested();
                    return solveSimpleNearestNeighbor(matrix, n, start);
                }));
            }
            
            // Recopilar resultats i trobar la millor solució
            TSPSolution bestSolution = null;
            double bestCost = Double.MAX_VALUE;
            
            for (Future<TSPSolution> future : futures) {
                try {
                    TSPSolution solution = future.get();
                    if (solution != null && solution.getTotalCost() < bestCost) {
                        bestCost = solution.getTotalCost();
                        bestSolution = solution;
                    }
                } catch (ExecutionException e) {
                    // Registrar error però continuar amb altres solucions
                    if (e.getCause() instanceof CancellationToken.CancellationException) {
                        throw (CancellationToken.CancellationException) e.getCause();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CancellationToken.CancellationException("Error");
                }
            }
            
            return bestSolution;
            
        } finally {
            executor.shutdownNow();
        }
    }
    
    /**
     * Solució bàsica utilitzant el veí més proper des d'una ciutat inicial específica.
     */
    private TSPSolution solveSimpleNearestNeighbor(double[][] matrix, int n, int startCity) {
        List<Integer> path = new ArrayList<>();
        boolean[] visited = new boolean[n];
        
        int currentCity = startCity;
        path.add(currentCity);
        visited[currentCity] = true;
        
        double totalCost = 0;
        
        // Visitar totes les altres ciutats
        for (int i = 1; i < n; i++) {
            int nextCity = -1;
            double minDistance = Double.MAX_VALUE;
            
            for (int j = 0; j < n; j++) {
                if (!visited[j] && matrix[currentCity][j] < minDistance) {
                    minDistance = matrix[currentCity][j];
                    nextCity = j;
                }
            }
            
            if (nextCity != -1) {
                path.add(nextCity);
                visited[nextCity] = true;
                totalCost += minDistance;
                currentCity = nextCity;
            }
        }
        
        // Retorn a l'inici
        totalCost += matrix[currentCity][startCity];
        path.add(startCity);
        
        return new TSPSolution(path, totalCost);
    }
    
    
    @Override
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }
    
    @Override
    public String getAlgorithmName() {
        return "Greedy";
    }
    
    /**
     * Obté la memòria utilitzada actualment.
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}