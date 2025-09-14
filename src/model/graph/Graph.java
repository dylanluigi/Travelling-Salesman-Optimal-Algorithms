package model.graph;

import model.visualization.GraphVisualizationData;

/**
 * Representa un graf complet dirigit amb pesos per al problema del viatjant de comerç.
 * 
 * Aquesta classe encapsula la matriu d'adjacència i proporciona mètodes per accedir
 * a les distàncies entre ciutats i generar dades de visualització.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class Graph {
    
    private final double[][] adjacencyMatrix;
    private final int size;
    
    /**
     * Constructor que crea un graf a partir d'una matriu d'adjacència.
     * 
     * @param adjacencyMatrix matriu quadrada amb les distàncies entre ciutats
     * @throws IllegalArgumentException si la matriu no és vàlida
     */
    public Graph(double[][] adjacencyMatrix) {
        validateMatrix(adjacencyMatrix);
        
        this.size = adjacencyMatrix.length;
        this.adjacencyMatrix = new double[size][size];
        
        // Copiar matriu per evitar modificacions externes
        for (int i = 0; i < size; i++) {
            System.arraycopy(adjacencyMatrix[i], 0, this.adjacencyMatrix[i], 0, size);
        }
    }
    
    /**
     * Obté la distància entre dues ciutats.
     * 
     * @param from ciutat d'origen (índex)
     * @param to ciutat de destí (índex)
     * @return distància entre les ciutats
     * @throws IndexOutOfBoundsException si els índexs no són vàlids
     */
    public double getDistance(int from, int to) {
        validateCityIndex(from);
        validateCityIndex(to);
        return adjacencyMatrix[from][to];
    }
    
    /**
     * Obté una còpia de la matriu d'adjacència.
     * 
     * @return còpia de la matriu d'adjacència
     */
    public double[][] getAdjacencyMatrix() {
        double[][] copy = new double[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(adjacencyMatrix[i], 0, copy[i], 0, size);
        }
        return copy;
    }
    
    /**
     * Obté el nombre de ciutats del graf.
     * 
     * @return nombre de ciutats
     */
    public int getSize() {
        return size;
    }
    
    /**
     * Calcula el cost total d'un camí donat.
     * 
     * @param path llista d'índexs de ciutats que representen el camí
     * @return cost total del camí
     * @throws IllegalArgumentException si el camí no és vàlid
     */
    public double calculatePathCost(java.util.List<Integer> path) {
        if (path == null || path.size() < 2) {
            throw new IllegalArgumentException("El camí ha de tenir almenys 2 ciutats");
        }
        
        double totalCost = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);
            totalCost += getDistance(from, to);
        }
        
        return totalCost;
    }
    
    /**
     * Genera dades de visualització per al graf.
     * 
     * @param solution solució TSP per destacar el camí òptim (pot ser null)
     * @return dades de visualització del graf
     */
    public GraphVisualizationData generateVisualizationData(TSPSolution solution) {
        GraphVisualizationData.Builder builder = new GraphVisualizationData.Builder(size);
        
        // Generar posicions circulars per les ciutats
        double centerX = 300;
        double centerY = 300;
        double radius = 200;
        
        for (int i = 0; i < size; i++) {
            double angle = 2 * Math.PI * i / size;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            
            builder.addCity(i, x, y, "Ciutat " + i);
        }
        
        // Afegir totes les arestes del graf
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    boolean isOptimal = false;
                    
                    // Marcar arestes de la solució òptima
                    if (solution != null) {
                        java.util.List<Integer> path = solution.getPath();
                        for (int k = 0; k < path.size() - 1; k++) {
                            if ((path.get(k) == i && path.get(k + 1) == j) ||
                                (k == path.size() - 2 && path.get(k + 1) == i && path.get(0) == j)) {
                                isOptimal = true;
                                break;
                            }
                        }
                    }
                    
                    builder.addEdge(i, j, adjacencyMatrix[i][j], isOptimal);
                }
            }
        }
        
        return builder.build();
    }
    
    /**
     * Obté estadístiques bàsiques del graf.
     * 
     * @return estadístiques del graf
     */
    public GraphStatistics getStatistics() {
        double minDistance = Double.MAX_VALUE;
        double maxDistance = 0;
        double totalDistance = 0;
        int edgeCount = 0;
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    double distance = adjacencyMatrix[i][j];
                    minDistance = Math.min(minDistance, distance);
                    maxDistance = Math.max(maxDistance, distance);
                    totalDistance += distance;
                    edgeCount++;
                }
            }
        }
        
        double averageDistance = totalDistance / edgeCount;
        
        return new GraphStatistics(size, edgeCount, minDistance, maxDistance, averageDistance);
    }
    
    /**
     * Verifica si el graf és simètric (no dirigit).
     * 
     * @return true si el graf és simètric
     */
    public boolean isSymmetric() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (Math.abs(adjacencyMatrix[i][j] - adjacencyMatrix[j][i]) > 1e-9) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Valida que la matriu d'adjacència sigui correcta.
     */
    private void validateMatrix(double[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("La matriu no pot ser buida");
        }
        
        int n = matrix.length;
        if (n < 3) {
            throw new IllegalArgumentException("El graf ha de tenir almenys 3 ciutats");
        }
        
        for (int i = 0; i < n; i++) {
            if (matrix[i].length != n) {
                throw new IllegalArgumentException("La matriu ha de ser quadrada");
            }
            
            for (int j = 0; j < n; j++) {
                if (i == j && matrix[i][j] != 0) {
                    throw new IllegalArgumentException("La diagonal ha de ser zero");
                }
                if (matrix[i][j] < 0) {
                    throw new IllegalArgumentException("Els pesos no poden ser negatius");
                }
                if (i != j && matrix[i][j] == 0) {
                    throw new IllegalArgumentException("No pot haver-hi arestes amb pes zero");
                }
            }
        }
    }
    
    /**
     * Valida que un índex de ciutat sigui vàlid.
     */
    private void validateCityIndex(int cityIndex) {
        if (cityIndex < 0 || cityIndex >= size) {
            throw new IndexOutOfBoundsException(
                "Índex de ciutat fora de rang: " + cityIndex + 
                " (vàlid: 0-" + (size - 1) + ")"
            );
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graf de ").append(size).append(" ciutats:\n");
        
        for (int i = 0; i < size; i++) {
            sb.append("Ciutat ").append(i).append(": ");
            for (int j = 0; j < size; j++) {
                sb.append(String.format("%8.2f ", adjacencyMatrix[i][j]));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Classe interna per encapsular estadístiques del graf.
     */
    public static class GraphStatistics {
        private final int numberOfCities;
        private final int numberOfEdges;
        private final double minDistance;
        private final double maxDistance;
        private final double averageDistance;
        
        public GraphStatistics(int numberOfCities, int numberOfEdges, 
                             double minDistance, double maxDistance, double averageDistance) {
            this.numberOfCities = numberOfCities;
            this.numberOfEdges = numberOfEdges;
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
            this.averageDistance = averageDistance;
        }
        
        // Mètodes d'accés
        public int getNumberOfCities() { return numberOfCities; }
        public int getNumberOfEdges() { return numberOfEdges; }
        public double getMinDistance() { return minDistance; }
        public double getMaxDistance() { return maxDistance; }
        public double getAverageDistance() { return averageDistance; }
        
        @Override
        public String toString() {
            return String.format(
                "Ciutats: %d, Arestes: %d, Distància mín: %.2f, màx: %.2f, mitjana: %.2f",
                numberOfCities, numberOfEdges, minDistance, maxDistance, averageDistance
            );
        }
    }
}