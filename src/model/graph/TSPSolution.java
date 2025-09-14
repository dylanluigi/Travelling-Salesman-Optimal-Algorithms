package model.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una solució al problema del viatjant de comerç (TSP).
 * 
 * Aquesta classe encapsula el camí òptim trobat, el cost total del camí,
 * i les estadístiques de l'execució de l'algoritme que ha generat la solució.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class TSPSolution {
    
    private final List<Integer> path;
    private final double totalCost;
    private TSPStatistics statistics;
    
    /**
     * Constructor que crea una solució TSP.
     * 
     * @param path camí que representa la seqüència de ciutats visitades
     * @param totalCost cost total del camí
     * @throws IllegalArgumentException si el camí és null o buit
     */
    public TSPSolution(List<Integer> path, double totalCost) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("El camí no pot ser null o buit");
        }
        if (totalCost < 0) {
            throw new IllegalArgumentException("El cost total no pot ser negatiu");
        }
        
        this.path = new ArrayList<>(path);
        this.totalCost = totalCost;
    }
    
    /**
     * Obté una còpia del camí de la solució.
     * 
     * @return llista amb la seqüència de ciutats del camí òptim
     */
    public List<Integer> getPath() {
        return new ArrayList<>(path);
    }
    
    /**
     * Obté el cost total de la solució.
     * 
     * @return cost total del camí òptim
     */
    public double getTotalCost() {
        return totalCost;
    }
    
    /**
     * Obté les estadístiques de l'execució de l'algoritme.
     * 
     * @return estadístiques de l'execució o null si no s'han establert
     */
    public TSPStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Estableix les estadístiques de l'execució de l'algoritme.
     * 
     * @param statistics estadístiques de l'execució
     */
    public void setStatistics(TSPStatistics statistics) {
        this.statistics = statistics;
    }
    
    /**
     * Obté el nombre de ciutats de la solució.
     * 
     * @return nombre de ciutats del camí
     */
    public int getNumberOfCities() {
        return path.size();
    }
    
    /**
     * Verifica si la solució és un cicle vàlid (torna a la ciutat inicial).
     * 
     * @return true si el camí forma un cicle complet
     */
    public boolean isValidCycle() {
        if (path.size() < 3) {
            return false;
        }
        
        // Verificar que la primera i última ciutat siguin la mateixa
        // o que sigui un camí que es pot tancar
        return path.get(0).equals(path.get(path.size() - 1)) || 
               path.size() >= 3; // Pot formar un cicle tancat
    }
    
    /**
     * Obté el camí complet incloent el retorn a la ciutat inicial.
     * 
     * @return camí complet amb retorn a l'origen
     */
    public List<Integer> getCompletePath() {
        List<Integer> completePath = new ArrayList<>(path);
        
        // Si el camí no torna a l'origen, afegir-hi la ciutat inicial
        if (!path.get(0).equals(path.get(path.size() - 1))) {
            completePath.add(path.get(0));
        }
        
        return completePath;
    }
    
    /**
     * Calcula el cost mitjà per aresta.
     * 
     * @return cost mitjà per aresta
     */
    public double getAverageCostPerEdge() {
        if (path.size() <= 1) {
            return 0;
        }
        
        int numberOfEdges = isValidCycle() ? path.size() : path.size() - 1;
        return totalCost / numberOfEdges;
    }
    
    /**
     * Compara aquesta solució amb una altra basant-se en el cost total.
     * 
     * @param other altra solució TSP per comparar
     * @return valor negatiu si aquesta solució és millor, 0 si són iguals, positiu si és pitjor
     */
    public int compareTo(TSPSolution other) {
        return Double.compare(this.totalCost, other.totalCost);
    }
    
    /**
     * Verifica si aquesta solució és millor (té menor cost) que una altra.
     * 
     * @param other altra solució per comparar
     * @return true si aquesta solució té menor cost
     */
    public boolean isBetterThan(TSPSolution other) {
        return this.totalCost < other.totalCost;
    }
    
    /**
     * Crea una representació en text del camí per a visualització.
     * 
     * @return string amb el camí formatat
     */
    public String getPathAsString() {
        if (path.isEmpty()) {
            return "Camí buit";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append("Ciutat ").append(path.get(i));
        }
        
        // Afegir retorn a l'origen si no està ja inclòs
        if (!path.get(0).equals(path.get(path.size() - 1))) {
            sb.append(" → Ciutat ").append(path.get(0));
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TSP Solution:\n");
        sb.append("  Camí: ").append(getPathAsString()).append("\n");
        sb.append("  Cost total: ").append(String.format("%.2f", totalCost)).append("\n");
        sb.append("  Nombre de ciutats: ").append(getNumberOfCities()).append("\n");
        sb.append("  Cost mitjà per aresta: ").append(String.format("%.2f", getAverageCostPerEdge()));
        
        if (statistics != null) {
            sb.append("\n  Estadístiques: ").append(statistics.toString());
        }
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TSPSolution other = (TSPSolution) obj;
        return Double.compare(totalCost, other.totalCost) == 0 && 
               path.equals(other.path);
    }
    
    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + Double.hashCode(totalCost);
        return result;
    }
}