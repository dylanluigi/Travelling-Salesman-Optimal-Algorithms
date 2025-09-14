package model.algorithm;

/**
 * Enumeració per als tipus d'algorismes de TSP.
 * 
 * Defineix tots els algorismes de TSP disponibles a l'aplicació,
 * incloent-hi el seu nom de visualització i una descripció detallada de
 * les seves característiques i aplicacions.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public enum AlgorithmType {
    /** Algorisme Força Bruta - examina totes les possibilitats */
    BRUTE_FORCE("Força Bruta (Complet)", "Algorisme exacte que examina TOTES les permutacions possibles. Garanteix la solució òptima absoluta. Límit pràctic: ~10 ciutats. Complexitat: O(n!)"),
    
    /** Algorisme Programació Dinàmica - Held-Karp */
    DYNAMIC_PROGRAMMING("Programació Dinàmica (Held-Karp)", "Algorisme exacte que utilitza programació dinàmica amb bitmasks. Molt més eficient que força bruta. Límit pràctic: ~20 ciutats. Complexitat: O(n² × 2ⁿ)"),
    
    /** Algorisme Ramificació i Poda clàssic */
    BRANCH_AND_BOUND("Ramificació i Poda", "Algorisme exacte que troba la solució òptima utilitzant tècniques de poda per reduir l'espai de cerca. Límit pràctic: ~15-25 ciutats. Complexitat: O(n!) cas pitjor"),
    
    /** Algorisme Ramificació i Poda concurrent */
    CONCURRENT_BRANCH_AND_BOUND("Ramificació i Poda Concurrent", "Implementació concurrent de Ramificació i Poda utilitzant ExecutorService per a execució paral·lela. Límit pràctic: ~15-20 ciutats"),
    
    
    /** Algorisme Voraç per comparació */
    GREEDY("Voraç (Veí més Proper)", "Algorisme heurístic ràpid que selecciona sempre l'aresta més curta disponible. No garanteix òptim però és molt ràpid. Límit pràctic: 1000+ ciutats. Complexitat: O(n²)");
    
    private final String name;
    private final String description;
    
    /**
     * Constructor per als valors de l'enumeració.
     * 
     * @param name nom d'visualització de l'algorisme
     * @param description descripció detallada de l'algorisme
     */
    AlgorithmType(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    /**
     * Obté el nom d'visualització de l'algorisme.
     * 
     * @return nom de l'algorisme
     */
    public String getName() {
        return name;
    }
    
    /**
     * Obté la descripció de l'algorisme.
     * 
     * Proporciona una explicació detallada de com funciona l'algorisme
     * i quines són les seves característiques principals.
     * 
     * @return descripció de l'algorisme
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Obté la representació en cadena de l'algorisme.
     * 
     * @return nom de l'algorisme
     */
    @Override
    public String toString() {
        return name;
    }
}