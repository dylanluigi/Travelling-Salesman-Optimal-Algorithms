package model.algorithm;

import model.graph.Graph;
import model.graph.TSPSolution;

/**
 * Interfície base per a tots els algorismes de TSP.
 * 
 * Defineix el contracte que han de complir tots els algorismes
 * per resoldre el problema del viatjant de comerç.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public interface TSPAlgorithm {
    
    /**
     * Resol el problema TSP per al graf donat.
     * 
     * @param graph graf que representa les ciutats i distàncies
     * @param withPruning si s'ha d'aplicar poda (aplicable segons l'algorisme)
     * @return solució òptima o aproximada trobada
     */
    TSPSolution solve(Graph graph, boolean withPruning);
    
    /**
     * Resol el problema TSP amb suport per cancel·lació.
     * 
     * @param graph graf que representa les ciutats i distàncies
     * @param withPruning si s'ha d'aplicar poda (aplicable segons l'algorisme)
     * @param cancellationToken token per cancel·lar l'execució
     * @return solució òptima o aproximada trobada
     * @throws CancellationToken.CancellationException si l'execució es cancel·la
     */
    default TSPSolution solve(Graph graph, boolean withPruning, CancellationToken cancellationToken) 
            throws CancellationToken.CancellationException {
        // Implementació per defecte que ignora el token de cancel·lació
        return solve(graph, withPruning);
    }
    
    /**
     * Estableix un callback per rebre actualitzacions de progrés.
     * 
     * @param callback callback per rebre updates de progrés
     */
    void setProgressCallback(ProgressCallback callback);
    
    /**
     * Obté el nom de l'algorisme.
     * 
     * @return nom de l'algorisme
     */
    String getAlgorithmName();
    
    /**
     * Interfície per rebre actualitzacions de progrés durant l'execució.
     */
    @FunctionalInterface
    interface ProgressCallback {
        void onProgress(int nodesExplored, int nodesPruned, double currentBound);
    }
}