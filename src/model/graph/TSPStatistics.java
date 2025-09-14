package model.graph;

/**
 * Classe que emmagatzema estadístiques sobre l'execució de l'algoritme TSP.
 * 
 * Inclou informació sobre el rendiment de l'algoritme com ara nodes explorats,
 * nodes descartats per poda, temps d'execució i ús de memòria.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class TSPStatistics {
    
    private int nodesExplored;
    private int nodesPruned;
    private long executionTime; // en mil·lisegons
    private double memoryUsed; // en MB
    private double bestBoundFound;
    private double initialBound;
    private int totalNodesGenerated;
    private double bestCost; // cost de la millor solució trobada
    
    /**
     * Constructor per defecte que inicialitza totes les estadístiques a zero.
     */
    public TSPStatistics() {
        this.nodesExplored = 0;
        this.nodesPruned = 0;
        this.executionTime = 0;
        this.memoryUsed = 0;
        this.bestBoundFound = Double.MAX_VALUE;
        this.initialBound = 0;
        this.totalNodesGenerated = 0;
        this.bestCost = 0;
    }
    
    /**
     * Constructor complet per inicialitzar totes les estadístiques.
     * 
     * @param nodesExplored nombre de nodes explorats durant la cerca
     * @param nodesPruned nombre de nodes descartats per poda
     * @param executionTime temps d'execució en mil·lisegons
     * @param memoryUsed memòria utilitzada en MB
     */
    public TSPStatistics(int nodesExplored, int nodesPruned, 
                        long executionTime, double memoryUsed) {
        this.nodesExplored = Math.max(0, nodesExplored);
        this.nodesPruned = Math.max(0, nodesPruned);
        this.executionTime = Math.max(0, executionTime);
        this.memoryUsed = Math.max(0, memoryUsed);
        this.bestBoundFound = Double.MAX_VALUE;
        this.initialBound = 0;
        this.totalNodesGenerated = nodesExplored + nodesPruned;
    }
    
    // Mètodes d'accés (getters)
    
    /**
     * Obté el nombre de nodes explorats durant la cerca.
     * 
     * @return nombre de nodes explorats
     */
    public int getNodesExplored() {
        return nodesExplored;
    }
    
    /**
     * Obté el nombre de nodes descartats per poda.
     * 
     * @return nombre de nodes podats
     */
    public int getNodesPruned() {
        return nodesPruned;
    }
    
    /**
     * Obté el temps d'execució en mil·lisegons.
     * 
     * @return temps d'execució en ms
     */
    public long getExecutionTime() {
        return executionTime;
    }
    
    /**
     * Obté la memòria utilitzada en MB.
     * 
     * @return memòria utilitzada en MB
     */
    public double getMemoryUsed() {
        return memoryUsed;
    }
    
    /**
     * Obté la millor cota trobada durant l'execució.
     * 
     * @return millor cota trobada
     */
    public double getBestBoundFound() {
        return bestBoundFound;
    }
    
    /**
     * Obté la cota inicial calculada.
     * 
     * @return cota inicial
     */
    public double getInitialBound() {
        return initialBound;
    }
    
    /**
     * Obté el nombre total de nodes generats (explorats + podats).
     * 
     * @return nombre total de nodes generats
     */
    public int getTotalNodesGenerated() {
        return totalNodesGenerated;
    }
    
    /**
     * Obté el cost de la millor solució trobada.
     * 
     * @return cost de la millor solució
     */
    public double getBestCost() {
        return bestCost;
    }
    
    // Mètodes de modificació (setters)
    
    /**
     * Estableix el nombre de nodes explorats.
     * 
     * @param nodesExplored nombre de nodes explorats
     */
    public void setNodesExplored(int nodesExplored) {
        this.nodesExplored = Math.max(0, nodesExplored);
        updateTotalNodes();
    }
    
    /**
     * Estableix el nombre de nodes podats.
     * 
     * @param nodesPruned nombre de nodes podats
     */
    public void setNodesPruned(int nodesPruned) {
        this.nodesPruned = Math.max(0, nodesPruned);
        updateTotalNodes();
    }
    
    /**
     * Estableix el temps d'execució.
     * 
     * @param executionTime temps d'execució en mil·lisegons
     */
    public void setExecutionTime(long executionTime) {
        this.executionTime = Math.max(0, executionTime);
    }
    
    /**
     * Estableix la memòria utilitzada.
     * 
     * @param memoryUsed memòria utilitzada en MB
     */
    public void setMemoryUsed(double memoryUsed) {
        this.memoryUsed = Math.max(0, memoryUsed);
    }
    
    /**
     * Estableix la millor cota trobada.
     * 
     * @param bestBoundFound millor cota trobada
     */
    public void setBestBoundFound(double bestBoundFound) {
        this.bestBoundFound = bestBoundFound;
    }
    
    /**
     * Estableix la cota inicial.
     * 
     * @param initialBound cota inicial
     */
    public void setInitialBound(double initialBound) {
        this.initialBound = initialBound;
    }
    
    /**
     * Estableix el cost de la millor solució trobada.
     * 
     * @param bestCost cost de la millor solució
     */
    public void setBestCost(double bestCost) {
        this.bestCost = bestCost;
    }
    
    // Mètodes d'increment de comptadors
    
    /**
     * Incrementa el comptador de nodes explorats.
     */
    public void incrementNodesExplored() {
        this.nodesExplored++;
        updateTotalNodes();
    }
    
    /**
     * Incrementa el comptador de nodes podats.
     */
    public void incrementNodesPruned() {
        this.nodesPruned++;
        updateTotalNodes();
    }
    
    /**
     * Incrementa el comptador de nodes explorats per una quantitat determinada.
     * 
     * @param count quantitat a incrementar
     */
    public void incrementNodesExplored(int count) {
        this.nodesExplored += Math.max(0, count);
        updateTotalNodes();
    }
    
    /**
     * Incrementa el comptador de nodes podats per una quantitat determinada.
     * 
     * @param count quantitat a incrementar
     */
    public void incrementNodesPruned(int count) {
        this.nodesPruned += Math.max(0, count);
        updateTotalNodes();
    }
    
    // Mètodes de càlcul d'estadístiques derivades
    
    /**
     * Calcula el percentatge de nodes podats respecte al total.
     * 
     * @return percentatge de poda (0-100)
     */
    public double getPruningPercentage() {
        if (totalNodesGenerated == 0) {
            return 0.0;
        }
        return (double) nodesPruned / totalNodesGenerated * 100.0;
    }
    
    /**
     * Calcula el percentatge de nodes explorats respecte al total.
     * 
     * @return percentatge d'exploració (0-100)
     */
    public double getExplorationPercentage() {
        if (totalNodesGenerated == 0) {
            return 0.0;
        }
        return (double) nodesExplored / totalNodesGenerated * 100.0;
    }
    
    /**
     * Calcula els nodes processats per segon.
     * 
     * @return nodes processats per segon
     */
    public double getNodesPerSecond() {
        if (executionTime == 0) {
            return 0.0;
        }
        return (double) totalNodesGenerated / (executionTime / 1000.0);
    }
    
    /**
     * Calcula la millora de la cota (si es coneix la cota inicial).
     * 
     * @return percentatge de millora de la cota
     */
    public double getBoundImprovement() {
        if (initialBound == 0 || bestBoundFound == Double.MAX_VALUE) {
            return 0.0;
        }
        return ((bestBoundFound - initialBound) / initialBound) * 100.0;
    }
    
    /**
     * Actualitza el total de nodes generats.
     */
    private void updateTotalNodes() {
        this.totalNodesGenerated = nodesExplored + nodesPruned;
    }
    
    /**
     * Reinicia totes les estadístiques.
     */
    public void reset() {
        this.nodesExplored = 0;
        this.nodesPruned = 0;
        this.executionTime = 0;
        this.memoryUsed = 0;
        this.bestBoundFound = Double.MAX_VALUE;
        this.initialBound = 0;
        this.totalNodesGenerated = 0;
    }
    
    /**
     * Crea una còpia de les estadístiques actuals.
     * 
     * @return nova instància amb els mateixos valors
     */
    public TSPStatistics copy() {
        TSPStatistics copy = new TSPStatistics(nodesExplored, nodesPruned, 
                                               executionTime, memoryUsed);
        copy.setBestBoundFound(bestBoundFound);
        copy.setInitialBound(initialBound);
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TSP Statistics:\n");
        sb.append("  Nodes explorats: ").append(nodesExplored).append("\n");
        sb.append("  Nodes podats: ").append(nodesPruned).append("\n");
        sb.append("  Total nodes generats: ").append(totalNodesGenerated).append("\n");
        sb.append("  Percentatge de poda: ").append(String.format("%.1f%%", getPruningPercentage())).append("\n");
        sb.append("  Temps d'execució: ").append(executionTime).append(" ms\n");
        sb.append("  Memòria utilitzada: ").append(String.format("%.2f MB", memoryUsed)).append("\n");
        sb.append("  Nodes per segon: ").append(String.format("%.0f", getNodesPerSecond()));
        
        if (bestBoundFound != Double.MAX_VALUE) {
            sb.append("\n  Millor cota trobada: ").append(String.format("%.2f", bestBoundFound));
        }
        
        return sb.toString();
    }
    
    /**
     * Retorna un resum compacte de les estadístiques.
     * 
     * @return resum de les estadístiques
     */
    public String toCompactString() {
        return String.format("Explorats: %d, Podats: %d (%.1f%%), Temps: %d ms", 
                           nodesExplored, nodesPruned, getPruningPercentage(), executionTime);
    }
}