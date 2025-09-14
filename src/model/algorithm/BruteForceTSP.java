package model.algorithm;

import model.graph.Graph;
import model.graph.TSPSolution;
import model.graph.TSPStatistics;

import java.util.*;

/**
 * Implementació Brute Force del TSP que avalua totes les rutes possibles.
 *
 * Aquest algorisme genera totes les permutacions de ciutats i n'avalua el cost
 * per trobar la solució òptima absoluta.
 *
 * Complexitat temporal: O(n!) (creixement factorial)
 * Complexitat espacial: O(n) per guardar la permutació actual
 * Límit pràctic: ~10 ciutats (10! = 3.628.800 permutacions)
 *
 * Ús recomanat:
 * - Instàncies petites on cal la solució òptima garantida
 * - Verificació de la correcció d'altres algorismes
 * - Propòsits acadèmics per entendre el cas pitjor
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class BruteForceTSP implements TSPAlgorithm {
    
    private ProgressCallback progressCallback;
    private TSPStatistics statistics;
    
    private static final int MAX_PRACTICAL_SIZE = 10;
    
    /**
     * Resol el problema mitjançant enumeració completa de totes les rutes.
     *
     * @param graph graf sobre el qual calcular la ruta òptima
     * @param withPruning no utilitzat en aquesta implementació
     * @return solució òptima trobada
     */
    @Override
    public TSPSolution solve(Graph graph, boolean withPruning) {
        return solve(graph, withPruning, CancellationToken.NONE);
    }
    
    /**
     * Resol el TSP enumerant totes les permutacions amb suport de cancel·lació.
     *
     * @param graph graf a resoldre
     * @param withPruning no utilitzat
     * @param cancellationToken token per interrompre l'execució
     * @return solució òptima resultant
     */
    @Override
    public TSPSolution solve(Graph graph, boolean withPruning, CancellationToken cancellationToken)
            throws CancellationToken.CancellationException {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        
        this.statistics = new TSPStatistics();
        int n = graph.getSize();
        double[][] matrix = graph.getAdjacencyMatrix();
        
        // Comprovació de seguretat pel límit pràctic
        if (n > MAX_PRACTICAL_SIZE) {
            throw new IllegalArgumentException(
                String.format("Brute force is not practical for %d cities (max recommended: %d). " +
                             "This would require evaluating %d! = %.0e permutations!", 
                             n, MAX_PRACTICAL_SIZE, n, factorial(n))
            );
        }
        
        // Generar totes les permutacions i trobar l'òptima
        TSPSolution solution = solveBruteForceComplete(matrix, n, cancellationToken);
        
        // Finalitzar les estadístiques
        statistics.setExecutionTime(System.currentTimeMillis() - startTime);
        statistics.setMemoryUsed((getUsedMemory() - startMemory) / (1024.0 * 1024.0));
        statistics.setBestCost(solution.getTotalCost());
        solution.setStatistics(statistics);
        
        return solution;
    }
    
    /**
     * Solució exhaustiva que avalua TOTES les rutes possibles.
     *
     * Genera totes les permutacions de les ciutats (excepte la inicial)
     * i avalua cada recorregut per trobar el cost mínim.
     */
    private TSPSolution solveBruteForceComplete(double[][] matrix, int n, CancellationToken cancellationToken) {
        List<Integer> cities = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            cities.add(i); // Ciutats 1, 2, ..., n-1 (la ciutat 0 és l'origen)
        }
        
        double bestCost = Double.MAX_VALUE;
        List<Integer> bestPath = null;
        
        // Generar TOTES les permutacions de ciutats (excepte la ciutat inicial)
        List<List<Integer>> allPermutations = generateAllPermutations(cities);
        long totalPermutations = allPermutations.size();
        
        
        long evaluatedCount = 0;
        for (List<Integer> permutation : allPermutations) {
            // Comprovar la cancel·lació a cada iteració
            cancellationToken.throwIfCancellationRequested();
            
            evaluatedCount++;
            statistics.incrementNodesExplored();
            
            // Crear el recorregut complet: 0 → permutació → 0
            List<Integer> completeTour = new ArrayList<>();
            completeTour.add(0);
            completeTour.addAll(permutation);
            completeTour.add(0);
            
            // Calcular el cost total d'aquest recorregut
            double tourCost = calculateTotalCost(completeTour, matrix);
            
            // Actualitzar la millor solució si aquesta és millor
            if (tourCost < bestCost) {
                bestCost = tourCost;
                bestPath = new ArrayList<>(completeTour);
                
            }
            
            // Actualitzar el progrés periòdicament
            if (progressCallback != null && evaluatedCount % Math.max(1, totalPermutations / 100) == 0) {
                double progress = (double) evaluatedCount / totalPermutations * 100;
                progressCallback.onProgress((int) evaluatedCount, 0, bestCost);
            }
        }
        
        
        return new TSPSolution(bestPath, bestCost);
    }
    
    /**
     * Genera totes les permutacions d'una llista mitjançant backtracking.
     *
     * Per a n ciutats es generen n! permutacions.
     */
    private List<List<Integer>> generateAllPermutations(List<Integer> cities) {
        List<List<Integer>> result = new ArrayList<>();
        generatePermutationsRecursive(new ArrayList<>(), new ArrayList<>(cities), result);
        return result;
    }
    
    /**
     * Mètode recursiu auxiliar per generar permutacions.
     */
    private void generatePermutationsRecursive(List<Integer> current, List<Integer> remaining, 
                                             List<List<Integer>> result) {
        if (remaining.isEmpty()) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = 0; i < remaining.size(); i++) {
            Integer city = remaining.get(i);
            
            // Afegir la ciutat a la permutació actual
            current.add(city);
            
            // Crear la llista de ciutats restants sense aquesta ciutat
            List<Integer> newRemaining = new ArrayList<>(remaining);
            newRemaining.remove(i);
            
            // Crida recursiva
            generatePermutationsRecursive(current, newRemaining, result);
            
            // Desfer pas (backtrack)
            current.remove(current.size() - 1);
        }
    }
    
    /**
     * Calcula el cost total d'un recorregut complet.
     * 
     * @param path llista que representa el camí a través de les ciutats
     * @param matrix matriu de distàncies entre ciutats
     * @return cost total del recorregut
     */
    private double calculateTotalCost(List<Integer> path, double[][] matrix) {
        if (path.size() < 2) return 0;
        
        double totalCost = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);
            totalCost += matrix[from][to];
        }
        return totalCost;
    }
    
    /**
     * Calcula el factorial per a l'estimació de complexitat.
     * 
     * @param n nombre per al qual calcular el factorial
     * @return valor del factorial de n
     */
    private double factorial(int n) {
        double result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
    
    /**
     * Obté l'ús actual de memòria.
     * 
     * @return memòria utilitzada en bytes
     */
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
        return "Brute Force (Complete Enumeration)";
    }
    
    /**
     * Obté la mida màxima pràctica per a aquest algoritme.
     * 
     * @return nombre màxim de ciutats recomanat per a aquest algoritme
     */
    public static int getMaxPracticalSize() {
        return MAX_PRACTICAL_SIZE;
    }
    
    /**
     * Estima el nombre de permutacions per a una mida donada.
     * 
     * @param n nombre de ciutats
     * @return nombre estimat de permutacions a processar
     */
    public static long estimatePermutations(int n) {
        if (n <= 1) return 1;
        long result = 1;
        for (int i = 2; i < n; i++) { // (n-1)! permutacions
            result *= i;
        }
        return result;
    }
}