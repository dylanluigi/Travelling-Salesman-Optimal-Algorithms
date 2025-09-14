package model.algorithm;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Fàbrica millorada per crear instàncies d'algorismes TSP.
 *
 * Utilitza patrons moderns de Java 8+ per facilitar l'extensibilitat i el rendiment.
 * Implementa el patró de fàbrica basat en {@code Supplier} per registrar
 * fàcilment nous algorismes.
 *
 * @author Dylan Canning Garcia
 * @version 2.0
 */
public class AlgorithmFactory {
    
    // Mapa de suppliers per crear instàncies d'algorismes de forma extensible
    private static final Map<AlgorithmType, Supplier<TSPAlgorithm>> ALGORITHM_SUPPLIERS = 
        Map.of(
            AlgorithmType.BRUTE_FORCE, BruteForceTSP::new,
            AlgorithmType.DYNAMIC_PROGRAMMING, HeldKarpTSP::new,
            AlgorithmType.BRANCH_AND_BOUND, BranchAndBound::new,
            AlgorithmType.CONCURRENT_BRANCH_AND_BOUND, ConcurrentBranchAndBound::new,
            AlgorithmType.GREEDY, GreedyTSP::new
        );
    
    /**
     * Crea una instància d'algorisme TSP segons el tipus especificat.
     *
     * @param type tipus d'algorisme a crear
     * @return instància de l'algorisme
     * @throws IllegalArgumentException si el tipus no és suportat
     */
    public static TSPAlgorithm createAlgorithm(AlgorithmType type) {
        Supplier<TSPAlgorithm> supplier = ALGORITHM_SUPPLIERS.get(type);
        if (supplier == null) {
            throw new IllegalArgumentException("Tipus d'algorisme no suportat: " + type);
        }
        return supplier.get();
    }
    
    /**
     * Crea un algorisme injectant una matriu de distàncies prèviament calculada.
     *
     * @param type tipus d'algorisme
     * @param distanceMatrix matriu de distàncies
     * @return instància d'algorisme configurada
     */
    public static TSPAlgorithm createAlgorithm(AlgorithmType type, double[][] distanceMatrix) {
        TSPAlgorithm algorithm = createAlgorithm(type);
        // En el futur es podria injectar la matriu de distàncies per optimitzar rendiment
        return algorithm;
    }
    
    /**
     * Obté un array de tots els tipus d'algorisme disponibles.
     * 
     * Retorna tots els valors de l'enumeració AlgorithmType, proporcionant
     * una manera convenient d'obtenir la llista completa d'algorismes
     * suportats per l'aplicació.
     * 
     * @return array de tipus d'algorisme disponibles
     */
    public static AlgorithmType[] getAvailableAlgorithms() {
        return AlgorithmType.values();
    }
    
    /**
     * Verifica si un tipus d'algorisme és compatible amb l'aplicació.
     * 
     * @param type tipus d'algorisme a verificar
     * @return true si l'algorisme és compatible
     */
    public static boolean isSupported(AlgorithmType type) {
        if (type == null) {
            return false;
        }
        
        try {
            createAlgorithm(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Obté informació sobre un algorisme sense crear una instància.
     * 
     * @param type tipus d'algorisme
     * @return informació de l'algorisme o null si no és compatible
     */
    public static AlgorithmInfo getAlgorithmInfo(AlgorithmType type) {
        if (!isSupported(type)) {
            return null;
        }
        
        return new AlgorithmInfo(type.getName(), type.getDescription(), type);
    }
    
    /**
     * Recomana l'algorisme més adequat segons la mida del problema.
     *
     * @param numberOfCities nombre de ciutats del problema
     * @return tipus d'algorisme recomanat
     */
    public static AlgorithmType recommendAlgorithm(int numberOfCities) {
        if (numberOfCities <= 8) {
            return AlgorithmType.BRUTE_FORCE; // Òptim garantit, temps raonable
        } else if (numberOfCities <= 15) {
            return AlgorithmType.DYNAMIC_PROGRAMMING; // Òptim i eficient
        } else if (numberOfCities <= 25) {
            return AlgorithmType.BRANCH_AND_BOUND; // Bon equilibri
        } else if (numberOfCities <= 50) {
            return AlgorithmType.CONCURRENT_BRANCH_AND_BOUND;
        } else {
            return AlgorithmType.GREEDY; // Opció pràctica per instàncies grans
        }
    }
    
    /**
     * Obté el límit pràctic recomanat per a cada algorisme.
     *
     * @param type tipus d'algorisme
     * @return nombre màxim recomanat de ciutats
     */
    public static int getPracticalLimit(AlgorithmType type) {
        switch (type) {
            case BRUTE_FORCE: return 10;
            case DYNAMIC_PROGRAMMING: return 20;
            case BRANCH_AND_BOUND: return 25;
            case CONCURRENT_BRANCH_AND_BOUND: return 20;
            case GREEDY: return Integer.MAX_VALUE;
            default: return 0;
        }
    }
    
    /**
     * Classe per encapsular informació sobre un algorisme.
     */
    public static class AlgorithmInfo {
        private final String name;
        private final String description;
        private final AlgorithmType type;
        
        /**
         * Constructor per crear una instància d'informació d'algorisme.
         * 
         * @param name nom de l'algorisme
         * @param description descripció de l'algorisme
         * @param type tipus d'algorisme
         */
        public AlgorithmInfo(String name, String description, AlgorithmType type) {
            this.name = name;
            this.description = description;
            this.type = type;
        }
        
        /**
         * Obté el nom de l'algorisme.
         * 
         * @return nom de l'algorisme
         */
        public String getName() { return name; }
        
        /**
         * Obté la descripció de l'algorisme.
         * 
         * @return descripció de l'algorisme
         */
        public String getDescription() { return description; }
        
        /**
         * Obté el tipus de l'algorisme.
         * 
         * @return tipus de l'algorisme
         */
        public AlgorithmType getType() { return type; }
        
        @Override
        public String toString() {
            return String.format("AlgorithmInfo{name='%s', type=%s}", name, type);
        }
    }
}