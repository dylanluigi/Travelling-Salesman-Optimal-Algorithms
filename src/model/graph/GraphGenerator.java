package model.graph;

import java.util.Random;

/**
 * Generador de grafs aleatoris per al problema del viatjant de comerç.
 * 
 * Aquesta classe proporciona mètodes per generar grafs amb diferents característiques:
 * - Grafs aleatoris amb distribució uniforme de pesos
 * - Grafs euclidians amb ciutats distribuïdes en un pla
 * - Grafs amb propietats específiques per a testing
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class GraphGenerator {
    
    private static final double DEFAULT_MIN_DISTANCE = 10.0;
    private static final double DEFAULT_MAX_DISTANCE = 100.0;
    
    /**
     * Genera un graf aleatori amb pesos uniformement distribuïts.
     * 
     * @param numCities nombre de ciutats del graf
     * @param seed llavor per a la generació aleatòria (per reproducibilitat)
     * @return nou graf amb distàncies aleatòries
     * @throws IllegalArgumentException si el nombre de ciutats no és vàlid
     */
    public Graph generateRandomGraph(int numCities, long seed) {
        return generateRandomGraph(numCities, seed, DEFAULT_MIN_DISTANCE, DEFAULT_MAX_DISTANCE);
    }
    
    /**
     * Genera un graf aleatori amb rang de distàncies personalitzat.
     * 
     * @param numCities nombre de ciutats del graf
     * @param seed llavor per a la generació aleatòria
     * @param minDistance distància mínima entre ciutats
     * @param maxDistance distància màxima entre ciutats
     * @return nou graf amb distàncies aleatòries
     * @throws IllegalArgumentException si els paràmetres no són vàlids
     */
    public Graph generateRandomGraph(int numCities, long seed, 
                                   double minDistance, double maxDistance) {
        validateParameters(numCities, minDistance, maxDistance);
        
        Random random = new Random(seed);
        double[][] adjacencyMatrix = new double[numCities][numCities];
        
        for (int i = 0; i < numCities; i++) {
            for (int j = 0; j < numCities; j++) {
                if (i == j) {
                    adjacencyMatrix[i][j] = 0;
                } else {
                    // Generar distància aleatòria dins del rang especificat
                    double distance = minDistance + 
                        (maxDistance - minDistance) * random.nextDouble();
                    adjacencyMatrix[i][j] = Math.round(distance * 100.0) / 100.0; // Arrodonir a 2 decimals
                }
            }
        }
        
        return new Graph(adjacencyMatrix);
    }
    
    /**
     * Genera un graf simètric (no dirigit) amb pesos aleatoris.
     * 
     * @param numCities nombre de ciutats del graf
     * @param seed llavor per a la generació aleatòria
     * @return nou graf simètric
     */
    public Graph generateSymmetricRandomGraph(int numCities, long seed) {
        return generateSymmetricRandomGraph(numCities, seed, DEFAULT_MIN_DISTANCE, DEFAULT_MAX_DISTANCE);
    }
    
    /**
     * Genera un graf simètric amb rang de distàncies personalitzat.
     * 
     * @param numCities nombre de ciutats del graf
     * @param seed llavor per a la generació aleatòria
     * @param minDistance distància mínima entre ciutats
     * @param maxDistance distància màxima entre ciutats
     * @return nou graf simètric
     */
    public Graph generateSymmetricRandomGraph(int numCities, long seed,
                                            double minDistance, double maxDistance) {
        validateParameters(numCities, minDistance, maxDistance);
        
        Random random = new Random(seed);
        double[][] adjacencyMatrix = new double[numCities][numCities];
        
        for (int i = 0; i < numCities; i++) {
            for (int j = i; j < numCities; j++) {
                if (i == j) {
                    adjacencyMatrix[i][j] = 0;
                } else {
                    // Generar distància aleatòria
                    double distance = minDistance + 
                        (maxDistance - minDistance) * random.nextDouble();
                    distance = Math.round(distance * 100.0) / 100.0;
                    
                    // Assignar la mateixa distància en ambdues direccions
                    adjacencyMatrix[i][j] = distance;
                    adjacencyMatrix[j][i] = distance;
                }
            }
        }
        
        return new Graph(adjacencyMatrix);
    }
    
    /**
     * Genera un graf euclidià amb ciutats distribuïdes aleatòriament en un pla.
     * 
     * @param numCities nombre de ciutats del graf
     * @param seed llavor per a la generació aleatòria
     * @param width amplada del pla (coordenades x: 0 a width)
     * @param height alçada del pla (coordenades y: 0 a height)
     * @return nou graf euclidià
     */
    public Graph generateEuclideanGraph(int numCities, long seed, 
                                      double width, double height) {
        if (numCities < 3 || numCities > 50) {
            throw new IllegalArgumentException("El nombre de ciutats ha d'estar entre 3 i 50");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Les dimensions han de ser positives");
        }
        
        Random random = new Random(seed);
        
        // Generar coordenades aleatòries per a cada ciutat
        double[][] coordinates = new double[numCities][2];
        for (int i = 0; i < numCities; i++) {
            coordinates[i][0] = width * random.nextDouble(); // x
            coordinates[i][1] = height * random.nextDouble(); // y
        }
        
        // Calcular matriu de distàncies euclidies
        double[][] adjacencyMatrix = new double[numCities][numCities];
        for (int i = 0; i < numCities; i++) {
            for (int j = 0; j < numCities; j++) {
                if (i == j) {
                    adjacencyMatrix[i][j] = 0;
                } else {
                    double dx = coordinates[i][0] - coordinates[j][0];
                    double dy = coordinates[i][1] - coordinates[j][1];
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    adjacencyMatrix[i][j] = Math.round(distance * 100.0) / 100.0;
                }
            }
        }
        
        return new Graph(adjacencyMatrix);
    }
    
    /**
     * Genera un graf euclidià estàndard amb dimensions 100x100.
     * 
     * @param numCities nombre de ciutats del graf
     * @param seed llavor per a la generació aleatòria
     * @return nou graf euclidià
     */
    public Graph generateEuclideanGraph(int numCities, long seed) {
        return generateEuclideanGraph(numCities, seed, 100.0, 100.0);
    }
    
    /**
     * Genera un graf circular amb ciutats distribuïdes uniformement en un cercle.
     * 
     * @param numCities nombre de ciutats del graf
     * @param radius radi del cercle
     * @return nou graf circular
     */
    public Graph generateCircularGraph(int numCities, double radius) {
        if (numCities < 3 || numCities > 50) {
            throw new IllegalArgumentException("El nombre de ciutats ha d'estar entre 3 i 50");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("El radi ha de ser positiu");
        }
        
        // Generar coordenades en cercle
        double[][] coordinates = new double[numCities][2];
        for (int i = 0; i < numCities; i++) {
            double angle = 2 * Math.PI * i / numCities;
            coordinates[i][0] = radius * Math.cos(angle); // x
            coordinates[i][1] = radius * Math.sin(angle); // y
        }
        
        // Calcular matriu de distàncies euclidies
        double[][] adjacencyMatrix = new double[numCities][numCities];
        for (int i = 0; i < numCities; i++) {
            for (int j = 0; j < numCities; j++) {
                if (i == j) {
                    adjacencyMatrix[i][j] = 0;
                } else {
                    double dx = coordinates[i][0] - coordinates[j][0];
                    double dy = coordinates[i][1] - coordinates[j][1];
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    adjacencyMatrix[i][j] = Math.round(distance * 100.0) / 100.0;
                }
            }
        }
        
        return new Graph(adjacencyMatrix);
    }
    
    /**
     * Genera un graf de test amb una solució òptima coneguda.
     * 
     * @param testCase tipus de cas de test
     * @return graf de test
     */
    public Graph generateTestGraph(TestGraphType testCase) {
        switch (testCase) {
            case SMALL_OPTIMAL:
                return generateSmallOptimalGraph();
            case TRIANGLE:
                return generateTriangleGraph();
            case SQUARE:
                return generateSquareGraph();
            case DEGENERATE:
                return generateDegenerateGraph();
            default:
                throw new IllegalArgumentException("Tipus de test no suportat: " + testCase);
        }
    }
    
    /**
     * Genera un graf petit amb solució òptima coneguda.
     */
    private Graph generateSmallOptimalGraph() {
        double[][] matrix = {
            {0, 10, 15, 20},
            {10, 0, 35, 25},
            {15, 35, 0, 30},
            {20, 25, 30, 0}
        };
        return new Graph(matrix);
    }
    
    /**
     * Genera un graf triangular de 3 ciutats.
     */
    private Graph generateTriangleGraph() {
        double[][] matrix = {
            {0, 10, 15},
            {12, 0, 20},
            {18, 25, 0}
        };
        return new Graph(matrix);
    }
    
    /**
     * Genera un graf quadrat de 4 ciutats.
     */
    private Graph generateSquareGraph() {
        double[][] matrix = {
            {0, 10, 20, 15},
            {12, 0, 18, 25},
            {22, 16, 0, 14},
            {17, 28, 12, 0}
        };
        return new Graph(matrix);
    }
    
    /**
     * Genera un graf degenerat per a testing d'edge cases.
     */
    private Graph generateDegenerateGraph() {
        double[][] matrix = {
            {0, 1, 100},
            {100, 0, 1},
            {1, 100, 0}
        };
        return new Graph(matrix);
    }
    
    /**
     * Valida els paràmetres d'entrada per a la generació de grafs.
     */
    private void validateParameters(int numCities, double minDistance, double maxDistance) {
        if (numCities < 3) {
            throw new IllegalArgumentException("El graf ha de tenir almenys 3 ciutats");
        }
        if (numCities > 20) {
            throw new IllegalArgumentException("El graf no pot tenir més de 20 ciutats per raons de rendiment");
        }
        if (minDistance < 0) {
            throw new IllegalArgumentException("La distància mínima no pot ser negativa");
        }
        if (maxDistance <= minDistance) {
            throw new IllegalArgumentException("La distància màxima ha de ser major que la mínima");
        }
    }
    
    /**
     * Enumeració dels tipus de grafs de test disponibles.
     */
    public enum TestGraphType {
        SMALL_OPTIMAL,  // Graf petit amb solució òptima coneguda
        TRIANGLE,       // Graf triangular de 3 ciutats
        SQUARE,         // Graf quadrat de 4 ciutats
        DEGENERATE      // Graf degenerat per testing
    }
}