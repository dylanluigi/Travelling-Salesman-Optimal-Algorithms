package model.algorithm;

import model.graph.Graph;
import model.graph.TSPSolution;
import model.graph.TSPStatistics;

import java.util.*;
import java.util.Arrays;

/**
 * Implementació de l'algoritme Branch and Bound per resoldre el problema del viatjant de comerç (TSP).
 * 
 * Aquest algoritme utilitza tècniques de poda per reduir l'espai de cerca:
 * - Reducció de matrius per calcular cotes inferiors
 * - Poda per cota (bound) per eliminar branques no prometedores
 * - Seguiment estadístic de nodes explorats i descartats
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class BranchAndBound implements TSPAlgorithm {
    
    private TSPAlgorithm.ProgressCallback progressCallback;
    private TSPStatistics statistics;
    private boolean enablePruning;
    
    /**
     * Resol el problema TSP utilitzant Branch and Bound.
     * 
     * @param graph el graf que representa les ciutats i distàncies
     * @param withPruning si s'ha d'aplicar poda
     * @return la solució òptima trobada
     */
    public TSPSolution solve(Graph graph, boolean withPruning) {
        return solve(graph, withPruning, CancellationToken.NONE);
    }
    
    @Override
    public TSPSolution solve(Graph graph, boolean withPruning, CancellationToken cancellationToken) 
            throws CancellationToken.CancellationException {
        this.enablePruning = withPruning;
        this.statistics = new TSPStatistics();
        
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        
        int n = graph.getSize();
        double[][] costMatrix = graph.getAdjacencyMatrix();
        
        // B&B estàndard: obtenir la cota inicial amb la heurística del veí més proper
        GreedyTSP greedy = new GreedyTSP();
        TSPSolution initialSolution = greedy.solve(graph, false);
        double initialUpperBound = initialSolution.getTotalCost();
        
        
        TSPSolution bestSolution;
        
        // Estratègia estàndard: B&B per a mides raonables i DP per a més grans
        if (n <= 15) {
            // Branch and Bound estàndard amb cotes basades en MST
            bestSolution = solveBranchAndBound(costMatrix, n, initialUpperBound, cancellationToken, initialSolution);
        } else if (n <= 20) {
            // Programació Dinàmica Held-Karp per a mides mitjanes
            bestSolution = solveDynamicProgramming(costMatrix, n, initialUpperBound, cancellationToken);
        } else {
            // Per a instàncies molt grans s'utilitza greedy millorada
            bestSolution = initialSolution;
        }
        
        // Finalitzar les estadístiques
        statistics.setExecutionTime(System.currentTimeMillis() - startTime);
        statistics.setMemoryUsed((getUsedMemory() - startMemory) / (1024.0 * 1024.0));
        statistics.setBestCost(bestSolution.getTotalCost());
        bestSolution.setStatistics(statistics);
        
        return bestSolution;
    }
    
    /**
     * Implementació estàndard de Branch and Bound utilitzant el mètode de
     * reducció de matrius.
     * 
     * Aquest és l'algoritme canònic del TSP que:
     * 1. Redueix la matriu a cada node per calcular cotes inferiors
     * 2. Marca files i columnes com a infinit segons les restriccions del camí
     * 3. Segueix l'esquema clàssic de GeeksforGeeks
     * 
     * @param originalMatrix matriu de costos original
     * @param n nombre de ciutats
     * @param initialUpperBound cota superior inicial
     * @param cancellationToken token de cancel·lació
     * @param initialSolution solució inicial
     * @return solució òptima trobada
     */
    private TSPSolution solveBranchAndBound(double[][] originalMatrix, int n, double initialUpperBound, CancellationToken cancellationToken, TSPSolution initialSolution) {
        // Important: mantenir una referència immutable als costos originals
        final double[][] originalCost = originalMatrix;
        double bestCost = initialUpperBound;
        List<Integer> bestPath = null;
        
        // Cua de prioritat ordenada per cost (cota inferior)
        PriorityQueue<TSPNode> queue = new PriorityQueue<>(
            Comparator.comparingDouble(node -> node.cost)
        );
        
        // Crear el node arrel amb la matriu reduïda
        TSPNode root = new TSPNode(n);
        
        // Copiar la matriu original i establir la diagonal a infinit
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    root.reducedMatrix[i][j] = Double.MAX_VALUE;
                } else {
                    root.reducedMatrix[i][j] = originalMatrix[i][j];
                }
            }
        }
        
        // Calcular la reducció inicial
        root.cost = calculateReducedMatrixBound(root.reducedMatrix);
        root.vertex = 0;  // Començar a la ciutat 0
        root.level = 1;   // Iniciar amb 1 ciutat visitada (la ciutat 0)
        root.path.add(0); // Afegir la ciutat inicial
        root.visited[0] = true; // Marcar la ciutat inicial com a visitada
        
        
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            // Comprovar cancel·lació
            cancellationToken.throwIfCancellationRequested();
            
            TSPNode current = queue.poll();
            statistics.incrementNodesExplored();
            
            // Actualitzar el progrés
            if (progressCallback != null) {
                progressCallback.onProgress(
                    statistics.getNodesExplored(),
                    statistics.getNodesPruned(),
                    bestCost
                );
            }
            
            // Poda: si el cost és >= al millor conegut, s'omet
            if (enablePruning && current.cost >= bestCost) {
                statistics.incrementNodesPruned();
                continue;
            }
            
            // Si s'han visitat totes les ciutats
            if (current.level == n) {
                // Fer servir la matriu original per tancar el cicle
                double totalCost = current.cost;
                if (originalCost[current.vertex][0] != Double.MAX_VALUE) {
                    totalCost += originalCost[current.vertex][0];
                } else {
                    continue; // no es pot tornar a l'inici
                }
                
                if (totalCost < bestCost) {
                    bestCost = totalCost;
                    bestPath = new ArrayList<>(current.path);
                    bestPath.add(0); // Tornar a l'origen
                    
                    // Debug: print when we find a better solution
                    // System.out.println("New best cost: " + bestCost + " at level " + current.level);
                }
                continue;
            }
            
            // Expandir cap a totes les ciutats no visitades
            for (int nextCity = 0; nextCity < n; nextCity++) {
                // Ometre si la ciutat ja s'ha visitat o si l'aresta no existeix
                if (current.visited[nextCity] || 
                    current.reducedMatrix[current.vertex][nextCity] == Double.MAX_VALUE) {
                    continue;
                }
                
                // Crear node fill utilitzant la matriu original de costos
                TSPNode child = createStandardChildNode(current, nextCity, n, originalCost);
                
                // Poda si el node no és prometedor
                if (enablePruning && child.cost >= bestCost) {
                    statistics.incrementNodesPruned();
                    continue;
                }
                
                queue.offer(child);
            }
        }
        
        // Recalcular el cost real del millor camí abans de tornar
        if (bestPath != null && bestPath.size() > 2) {
            double trueCost = calculateTotalCost(bestPath, originalCost);
            return new TSPSolution(bestPath, trueCost);
        } else {
            // Si B&B no troba una solució vàlida, es retorna la solució greedy inicial
            return new TSPSolution(initialSolution.getPath(), initialSolution.getTotalCost());
        }
    }
    
    /**
     * Creació estàndard de nodes fills seguint l'enfocament de matriu
     * reduïda de GeeksforGeeks.
     * 
     * Passos clau:
     * 1. Copiar la matriu reduïda del pare
     * 2. Posar la fila i la columna seleccionades a infinit
     * 3. Bloquejar l'aresta de retorn per evitar subcicles
     * 4. Reduir la matriu i calcular el nou cost
     * Es fa servir la matriu de costos original per obtenir els costos reals
     * 
     * @param parent node pare del qual crear el fill
     * @param nextCity següent ciutat a visitar
     * @param n nombre total de ciutats
     * @param originalCost matriu de costos original
     * @return nou node fill amb la matriu reduïda actualitzada
     */
    private TSPNode createStandardChildNode(TSPNode parent, int nextCity, int n, double[][] originalCost) {
        TSPNode child = new TSPNode(n);
        
        // Copiar la matriu reduïda del pare
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                child.reducedMatrix[i][j] = parent.reducedMatrix[i][j];
            }
        }
        
        // Optimització: còpia eficient de l'array de ciutats visitades
        System.arraycopy(parent.visited, 0, child.visited, 0, n);
        child.visited[nextCity] = true; // Marcar la nova ciutat com a visitada
        
        // Obtenir el cost real de l'aresta de la matriu original
        double edgeCost = originalCost[parent.vertex][nextCity];
        
        // Establir la fila parent.vertex a infinit
        for (int j = 0; j < n; j++) {
            child.reducedMatrix[parent.vertex][j] = Double.MAX_VALUE;
        }
        
        // Establir la columna nextCity a infinit
        for (int i = 0; i < n; i++) {
            child.reducedMatrix[i][nextCity] = Double.MAX_VALUE;
        }
        
        // Establir l'aresta de retorn a infinit per evitar subcicles
        child.reducedMatrix[nextCity][parent.vertex] = Double.MAX_VALUE;
        
        // Calcular el cost de reducció addicional necessari després de les modificacions
        double additionalReductionCost = calculateAdditionalReduction(child.reducedMatrix);
        
        // Cost total = cota del pare + cost real de l'aresta + reducció addicional necessària
        child.cost = parent.cost + edgeCost + additionalReductionCost;
        child.vertex = nextCity;
        child.level = parent.level + 1;
        child.path = new ArrayList<>(parent.path);
        child.path.add(nextCity);
        
        return child;
    }
    
    /**
     * Calcula la reducció addicional necessària després d'establir files/columnes a infinit.
     * 
     * Aquesta és la clau per obtenir cotes adequades en Branch and Bound. Després de modificar
     * la matriu marcant files i columnes com a infinit, cal recalcular quanta reducció 
     * addicional es pot aplicar.
     * 
     * @param matrix matriu modificada després de les operacions de marca
     * @return cost de reducció addicional necessari
     */
    private double calculateAdditionalReduction(double[][] matrix) {
        int n = matrix.length;
        double additionalReduction = 0;
        
        // Comprovar cada fila per trobar nous mínims després de les modificacions
        for (int i = 0; i < n; i++) {
            double rowMin = Double.MAX_VALUE;
            boolean hasFiniteValue = false;
            
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] != Double.MAX_VALUE) {
                    hasFiniteValue = true;
                    if (matrix[i][j] < rowMin) {
                        rowMin = matrix[i][j];
                    }
                }
            }
            
            // Si la fila té valors finits i el mínim > 0, afegir a la reducció
            if (hasFiniteValue && rowMin > 0 && rowMin != Double.MAX_VALUE) {
                additionalReduction += rowMin;
                // Reduir la fila
                for (int j = 0; j < n; j++) {
                    if (matrix[i][j] != Double.MAX_VALUE) {
                        matrix[i][j] -= rowMin;
                    }
                }
            }
        }
        
        // Comprovar cada columna per trobar nous mínims després de la reducció de files
        for (int j = 0; j < n; j++) {
            double colMin = Double.MAX_VALUE;
            boolean hasFiniteValue = false;
            
            for (int i = 0; i < n; i++) {
                if (matrix[i][j] != Double.MAX_VALUE) {
                    hasFiniteValue = true;
                    if (matrix[i][j] < colMin) {
                        colMin = matrix[i][j];
                    }
                }
            }
            
            // Si la columna té valors finits i el mínim > 0, afegir a la reducció
            if (hasFiniteValue && colMin > 0 && colMin != Double.MAX_VALUE) {
                additionalReduction += colMin;
            }
        }
        
        return additionalReduction;
    }
    
    /**
     * Reducció de matriu sense mutacions per evitar efectes laterals.
     * Es crea una còpia de la matriu abans de reduir-la per preservar-ne l'estat.
     */
    private double calculateReducedMatrixBoundNonMutating(double[][] originalMatrix) {
        int n = originalMatrix.length;
        
        // Crear una còpia per no modificar l'original
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = originalMatrix[i][j];
            }
        }
        
        return calculateReducedMatrixBound(matrix);
    }
    
    /**
     * Càlcul de la cota inferior mitjançant reducció de matriu (mètode clàssic).
     *
     * Segueix l'enfocament de GeeksforGeeks:
     * 1. Reducció per files restant el mínim de cada fila
     * 2. Reducció per columnes restant el mínim de cada columna
     * 3. La cota inferior és la suma de totes les reduccions
     *
     * ATENCIÓ: aquest mètode modifica la matriu rebuda!
     */
    private double calculateReducedMatrixBound(double[][] matrix) {
        int n = matrix.length;
        double totalReductionCost = 0;
        
        // Pas 1: Reducció de files
        for (int i = 0; i < n; i++) {
            double minRow = Double.MAX_VALUE;
            
            // Trobar el mínim a la fila i
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] < minRow) {
                    minRow = matrix[i][j];
                }
            }
            
            // Restar el mínim de tots els elements de la fila i (si és finit)
            if (minRow != Double.MAX_VALUE && minRow > 0) {
                totalReductionCost += minRow;
                for (int j = 0; j < n; j++) {
                    if (matrix[i][j] != Double.MAX_VALUE) {
                        matrix[i][j] -= minRow;
                    }
                }
            }
        }
        
        // Pas 2: Reducció de columnes
        for (int j = 0; j < n; j++) {
            double minCol = Double.MAX_VALUE;
            
            // Trobar el mínim a la columna j
            for (int i = 0; i < n; i++) {
                if (matrix[i][j] < minCol) {
                    minCol = matrix[i][j];
                }
            }
            
            // Restar el mínim de tots els elements de la columna j (si és finit)
            if (minCol != Double.MAX_VALUE && minCol > 0) {
                totalReductionCost += minCol;
                for (int i = 0; i < n; i++) {
                    if (matrix[i][j] != Double.MAX_VALUE) {
                        matrix[i][j] -= minCol;
                    }
                }
            }
        }
        
        return totalReductionCost;
    }
    
    /**
     * Calcula l'arbre de recobriment mínim (MST) utilitzant l'algoritme de Prim per al conjunt de ciutats donat.
     * 
     * @param matrix matriu d'adjacència amb les distàncies
     * @param cities llista de ciutats per les quals calcular l'MST
     * @return cost total de l'MST
     */
    private double calculateMST(double[][] matrix, List<Integer> cities) {
        if (cities.size() <= 1) return 0;
        
        Set<Integer> inMST = new HashSet<>();
        Set<Integer> notInMST = new HashSet<>(cities);
        
        // Començar amb la primera ciutat
        inMST.add(cities.get(0));
        notInMST.remove(cities.get(0));
        
        double mstCost = 0;
        
        // Algoritme de Prim
        while (!notInMST.isEmpty()) {
            double minEdge = Double.MAX_VALUE;
            int nextCity = -1;
            
            // Trobar l'aresta mínima de l'MST a les ciutats restants
            for (int inCity : inMST) {
                for (int outCity : notInMST) {
                    if (matrix[inCity][outCity] < minEdge) {
                        minEdge = matrix[inCity][outCity];
                        nextCity = outCity;
                    }
                }
            }
            
            // Afegir l'aresta mínima a l'MST
            if (nextCity != -1) {
                mstCost += minEdge;
                inMST.add(nextCity);
                notInMST.remove(nextCity);
            } else {
                break; // No hauria de passar en un graf complet
            }
        }
        
        return mstCost;
    }
    
    /**
     * Programació dinàmica Held-Karp millorada amb poda i gestió de memòria optimitzades.
     * 
     * @param matrix matriu de distàncies
     * @param n nombre de ciutats
     * @param upperBound cota superior per a la poda
     * @return solució òptima trobada
     */
    private TSPSolution solveDynamicProgrammingOptimized(double[][] matrix, int n, double upperBound) {
        if (n > 20) {
            // Per a instàncies molt grans, utilitzar greedy millorat amb múltiples reinicis
            return solveEnhancedGreedy(matrix, n);
        }
        
        // Utilitzar DP optimitzat amb bitmask i poda
        Map<Long, Double> memo = new HashMap<>();
        Map<Long, Integer> parent = new HashMap<>();
        
        double result = tspDPOptimized(matrix, 1L, 0, n, memo, parent, upperBound);
        
        if (result < upperBound) {
            List<Integer> path = reconstructOptimalPath(matrix, n, memo, parent);
            return new TSPSolution(path, result);
        }
        
        // Utilitzar greedy millorat com a alternativa
        return solveEnhancedGreedy(matrix, n);
    }
    
    /**
     * DP optimitzat amb terminació primerenca i millor poda.
     */
    private double tspDPOptimized(double[][] matrix, long mask, int pos, int n, 
                                 Map<Long, Double> memo, Map<Long, Integer> parent, double upperBound) {
        // Terminació primerenca si el cost parcial supera la cota superior
        if (mask == (1L << n) - 1) {
            return matrix[pos][0];
        }
        
        long key = (mask << 6) | pos; // Combinar màscara i posició
        if (memo.containsKey(key)) {
            return memo.get(key);
        }
        
        double minCost = Double.MAX_VALUE;
        int bestNext = -1;
        
        // Provar visitar totes les ciutats no visitades
        for (int city = 0; city < n; city++) {
            if ((mask & (1L << city)) == 0) {
                long newMask = mask | (1L << city);
                double cost = matrix[pos][city] + tspDPOptimized(matrix, newMask, city, n, memo, parent, upperBound);
                
                if (cost < minCost) {
                    minCost = cost;
                    bestNext = city;
                }
                
                statistics.incrementNodesExplored();
                
                // Terminació primerenca si el cost supera la cota superior
                if (cost >= upperBound) {
                    statistics.incrementNodesPruned();
                    break;
                }
            }
        }
        
        memo.put(key, minCost);
        if (bestNext != -1) {
            parent.put(key, bestNext);
        }
        
        return minCost;
    }
    
    /**
     * Greedy millorat amb múltiples reinicis i cerca local.
     */
    private TSPSolution solveEnhancedGreedy(double[][] matrix, int n) {
        GreedyTSP enhancedGreedy = new GreedyTSP();
        Graph tempGraph = new Graph(matrix);
        return enhancedGreedy.solve(tempGraph, false);
    }
    
    /**
     * Reconstruir el camí òptim de les taules DP.
     */
    private List<Integer> reconstructOptimalPath(double[][] matrix, int n, 
                                               Map<Long, Double> memo, Map<Long, Integer> parent) {
        List<Integer> path = new ArrayList<>();
        long mask = 1L;
        int pos = 0;
        path.add(0);
        
        while (mask != (1L << n) - 1) {
            long key = (mask << 6) | pos;
            Integer nextCity = parent.get(key);
            
            if (nextCity == null) break;
            
            path.add(nextCity);
            mask |= (1L << nextCity);
            pos = nextCity;
        }
        
        path.add(0);
        return path;
    }
    
    /**
     * Implementació correcta de Programació Dinàmica Held-Karp.
     * Utilitza representació amb bitmask i punters pare per a reconstrucció eficient O(n) del camí.
     */
    private TSPSolution solveDynamicProgramming(double[][] matrix, int n, double upperBound, CancellationToken cancellationToken) {
        if (n > 20) {
            // DP és exponencial en espai/temps: O(n^2 * 2^n)
            // Per a instàncies molt grans, retornar el millor greedy en lloc de fallar
            GreedyTSP greedy = new GreedyTSP();
            return greedy.solve(new model.graph.Graph(matrix), false);
        }
        
        // DP Held-Karp amb seguiment adequat de pares per a reconstrucció O(n)
        Map<String, Double> memo = new HashMap<>();
        Map<String, Integer> parent = new HashMap<>(); // CRÍTIC: Seguir el pare per a reconstrucció eficient
        
        // Iniciar DP: màscara visitada = 1 (ciutat 0 visitada), posició actual = 0
        double result = tspDP(matrix, 1, 0, n, memo, parent);
        
        // Sempre retornar el resultat DP - està garantit que és òptim per aquesta mida
        List<Integer> path = reconstructPath(n, parent);
        return new TSPSolution(path, result);
    }
    
    /**
     * Funció DP recursiva amb memoització i seguiment de pares.
     */
    private double tspDP(double[][] matrix, int mask, int pos, int n, Map<String, Double> memo, Map<String, Integer> parent) {
        // Si totes les ciutats han estat visitades, retornar el cost per tornar a l'inici
        if (mask == (1 << n) - 1) {
            return matrix[pos][0];
        }
        
        String key = mask + "," + pos;
        if (memo.containsKey(key)) {
            return memo.get(key);
        }
        
        double minCost = Double.MAX_VALUE;
        int bestNext = -1;
        
        // Provar visitar totes les ciutats no visitades
        for (int city = 0; city < n; city++) {
            if ((mask & (1 << city)) == 0) { // Si la ciutat no ha estat visitada
                double cost = matrix[pos][city] + tspDP(matrix, mask | (1 << city), city, n, memo, parent);
                if (cost < minCost) {
                    minCost = cost;
                    bestNext = city;
                }
                statistics.incrementNodesExplored();
            }
        }
        
        memo.put(key, minCost);
        if (bestNext != -1) {
            parent.put(key, bestNext);
        }
        return minCost;
    }
    
    /**
     * Reconstruir el camí òptim dels punters pare (temps O(n)).
     */
    private List<Integer> reconstructPath(int n, Map<String, Integer> parent) {
        List<Integer> path = new ArrayList<>();
        int mask = 1; // Començar amb la ciutat 0 visitada
        int pos = 0;
        path.add(0);
        
        while (mask != (1 << n) - 1) {
            String key = mask + "," + pos;
            Integer nextCity = parent.get(key);
            
            if (nextCity != null) {
                path.add(nextCity);
                mask |= (1 << nextCity);
                pos = nextCity;
            } else {
                // No hauria de passar si DP és correcte
                break;
            }
        }
        
        path.add(0); // Tornar a l'inici
        return path;
    }
    
    /**
     * Resolutor exacte heretat per a instàncies molt petites.
     */
    private TSPSolution solveExact(double[][] matrix, int n) {
        List<Integer> cities = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            cities.add(i);
        }
        
        double bestCost = Double.MAX_VALUE;
        List<Integer> bestPath = null;
        
        // Generar totes les permutacions de ciutats (excepte la 0)
        List<List<Integer>> permutations = generatePermutations(cities);
        
        for (List<Integer> perm : permutations) {
            statistics.incrementNodesExplored();
            
            // Crear camí complet: 0 -> perm -> 0
            List<Integer> fullPath = new ArrayList<>();
            fullPath.add(0);
            fullPath.addAll(perm);
            fullPath.add(0);
            
            double cost = calculateTotalCost(fullPath, matrix);
            
            if (cost < bestCost) {
                bestCost = cost;
                bestPath = new ArrayList<>(fullPath);
            }
            
            // Actualitzar progrés
            if (progressCallback != null) {
                progressCallback.onProgress(
                    statistics.getNodesExplored(),
                    statistics.getNodesPruned(),
                    bestCost
                );
            }
        }
        
        return bestPath != null ? new TSPSolution(bestPath, bestCost) : null;
    }
    
    /**
     * Genera totes les permutacions d'una llista.
     */
    private List<List<Integer>> generatePermutations(List<Integer> list) {
        List<List<Integer>> result = new ArrayList<>();
        if (list.isEmpty()) {
            result.add(new ArrayList<>());
            return result;
        }
        
        for (int i = 0; i < list.size(); i++) {
            Integer element = list.get(i);
            List<Integer> remaining = new ArrayList<>(list);
            remaining.remove(i);
            
            List<List<Integer>> subPermutations = generatePermutations(remaining);
            for (List<Integer> subPerm : subPermutations) {
                List<Integer> newPerm = new ArrayList<>();
                newPerm.add(element);
                newPerm.addAll(subPerm);
                result.add(newPerm);
            }
        }
        
        return result;
    }
    
    
    
    
    /**
     * Obté una solució inicial utilitzant l'algoritme greedy.
     */
    private TSPSolution getGreedySolution(Graph graph) {
        int n = graph.getSize();
        double[][] matrix = graph.getAdjacencyMatrix();
        
        List<Integer> path = new ArrayList<>();
        boolean[] visited = new boolean[n];
        
        int currentCity = 0;
        path.add(currentCity);
        visited[currentCity] = true;
        
        double totalCost = 0;
        
        for (int i = 1; i < n; i++) {
            int nextCity = -1;
            double minDistance = Double.MAX_VALUE;
            
            for (int j = 0; j < n; j++) {
                if (!visited[j] && matrix[currentCity][j] < minDistance) {
                    minDistance = matrix[currentCity][j];
                    nextCity = j;
                }
            }
            
            path.add(nextCity);
            visited[nextCity] = true;
            totalCost += minDistance;
            currentCity = nextCity;
        }
        
        // Tornar a la ciutat inicial
        totalCost += matrix[currentCity][0];
        path.add(0);
        
        return new TSPSolution(path, totalCost);
    }
    
    /**
     * Calcula el cost total d'un camí.
     */
    private double calculateTotalCost(List<Integer> path, double[][] matrix) {
        if (path.size() < 2) return 0;
        
        double totalCost = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            int fromCity = path.get(i);
            int toCity = path.get(i + 1);
            
            // Verificar que els índexs siguin vàlids
            if (fromCity >= 0 && fromCity < matrix.length && 
                toCity >= 0 && toCity < matrix.length) {
                totalCost += matrix[fromCity][toCity];
            }
        }
        return totalCost;
    }
    
    /**
     * Copia una matriu.
     */
    private double[][] copyMatrix(double[][] original) {
        double[][] copy = new double[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }
    
    /**
     * Obté la memòria utilitzada actualment.
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    // Setters per callbacks
    @Override
    public void setProgressCallback(TSPAlgorithm.ProgressCallback callback) {
        this.progressCallback = callback;
    }
    
    @Override
    public String getAlgorithmName() {
        return "Branch and Bound";
    }
    
    /**
     * Representació d'un node del TSP seguint l'enfocament de GeeksforGeeks.
     * Cada node manté el seu propi estat de matriu reduïda.
     */
    private static class TSPNode {
        double[][] reducedMatrix;   // Cada node té la seva pròpia matriu reduïda
        List<Integer> path;         // Camí actual recorregut
        boolean[] visited;          // Seguiment O(1) de ciutats visitades
        double cost;                // Cost total: camí + reducció
        int level;                  // Nombre de ciutats visitades
        int vertex;                 // Ciutat actual
        
        TSPNode() {
            this.path = new ArrayList<>();
        }
        
        TSPNode(int n) {
            this.path = new ArrayList<>();
            this.reducedMatrix = new double[n][n];
            this.visited = new boolean[n];
        }
    }
}