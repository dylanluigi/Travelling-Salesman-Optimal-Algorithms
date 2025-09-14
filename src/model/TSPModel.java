package model;

import model.algorithm.AlgorithmFactory;
import model.algorithm.AlgorithmType;
import model.algorithm.TSPAlgorithm;
import model.graph.Graph;
import model.graph.GraphGenerator;
import model.graph.TSPSolution;
import notification.NotificationService;
import model.visualization.GraphVisualizationData;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implementació de la capa de model per a l'aplicació TSP.
 * 
 * Aquesta classe gestiona la càrrega de grafs, l'execució de l'algoritme Branch and Bound
 * amb poda per resoldre el problema del viatjant de comerç, i la generació de dades per
 * a visualitzacions. Utilitza un pool de fils per optimitzar els càlculs computacionalment
 * intensius.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class TSPModel {
    private final NotificationService notificationService;
    private final ExecutorService executorService;
    private TSPAlgorithm currentAlgorithm;
    private AlgorithmType algorithmType;
    private final GraphGenerator graphGenerator;
    
    private Graph currentGraph;
    private TSPSolution lastSolution;
    
    private final List<ModelObserver> observers = new ArrayList<>();
    
    /**
     * Constructor que inicialitza el model amb un servei de notificacions.
     * 
     * Configura un pool de fils optimitzat per al processament en paral·lel dels càlculs
     * de l'algoritme Branch and Bound.
     * 
     * @param notificationService servei per publicar esdeveniments del model
     */
    public TSPModel(NotificationService notificationService) {
        this.notificationService = notificationService;
        this.executorService = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors())
        );
        this.algorithmType = AlgorithmType.BRANCH_AND_BOUND;
        this.currentAlgorithm = AlgorithmFactory.createAlgorithm(algorithmType);
        this.graphGenerator = new GraphGenerator();
    }
    
    /**
     * Carrega un graf des d'una matriu d'adjacència.
     * 
     * @param adjacencyMatrix matriu d'adjacència que representa el graf
     * @throws IllegalArgumentException si la matriu no és vàlida
     */
    public void loadGraphFromMatrix(double[][] adjacencyMatrix) {
        validateAdjacencyMatrix(adjacencyMatrix);
        
        this.currentGraph = new Graph(adjacencyMatrix);
        notifyObservers(observer -> observer.onGraphLoaded(currentGraph));
        
        // Publicar esdeveniment de càrrega completada
        NotificationService.Event event = new NotificationService.Event(NotificationService.EventType.INFO,
            "Graf carregat amb " + currentGraph.getSize() + " ciutats");
        notificationService.publishEvent(event);
    }
    
    /**
     * Genera un graf aleatori amb el nombre especificat de ciutats.
     * 
     * @param numCities nombre de ciutats del graf
     * @param seed llavor per a la generació aleatòria
     * @throws IllegalArgumentException si el nombre de ciutats no és vàlid
     */
    public void generateRandomGraph(int numCities, long seed) {
        if (numCities < 3 || numCities > 50) {
            throw new IllegalArgumentException("El nombre de ciutats ha d'estar entre 3 i 50");
        }
        
        this.currentGraph = graphGenerator.generateRandomGraph(numCities, seed);
        notifyObservers(observer -> observer.onGraphLoaded(currentGraph));

        NotificationService.Event event = new  NotificationService.Event(NotificationService.EventType.INFO,
            "Graf aleatori generat amb " + numCities + " ciutats");
        notificationService.publishEvent(event);
    }
    
    /**
     * Executa l'algoritme Branch and Bound de manera asíncrona.
     * 
     * @param withPruning si s'ha d'aplicar poda
     * @return Future que representa l'execució asíncrona
     */
    public Future<?> solveTSPAsync(boolean withPruning) {
        if (currentGraph == null) {
            throw new IllegalStateException("No hi ha cap graf carregat");
        }
        
        return executorService.submit(() -> {
            try {
                // Configurar l'algoritme amb callbacks per actualitzacions de progrés
                currentAlgorithm.setProgressCallback((nodesExplored, nodesPruned, currentBound) -> {
                    notifyObservers(observer -> 
                        observer.onProgressUpdate(nodesExplored, nodesPruned, currentBound));
                });
                
                // Executar l'algoritme
                TSPSolution solution = currentAlgorithm.solve(currentGraph, withPruning);
                this.lastSolution = solution;
                
                // Notificar solució trobada
                notifyObservers(observer -> observer.onSolutionFound(solution));

                NotificationService.Event event = new NotificationService.Event(NotificationService.EventType.INFO,
                    "Solució trobada: cost " + solution.getTotalCost() + 
                    ", nodes explorats: " + solution.getStatistics().getNodesExplored());
                notificationService.publishEvent(event);
                
            } catch (Exception e) {
                notifyObservers(observer -> observer.onComputationError(e.getMessage()));

                NotificationService.Event event = new NotificationService.Event(NotificationService.EventType.ERROR,
                    "Error durant l'execució: " + e.getMessage());
                notificationService.publishEvent(event);
            }
        });
    }
    
    /**
     * Resol el problema TSP de forma asíncrona amb suport per cancel·lació.
     * 
     * @param withPruning si s'ha d'aplicar poda
     * @param cancellationToken token per cancel·lar l'execució
     * @return Future que representa l'execució asíncrona
     */
    public Future<?> solveTSPAsyncWithCancellation(boolean withPruning, model.algorithm.CancellationToken cancellationToken) {
        if (currentGraph == null) {
            throw new IllegalStateException("No hi ha cap graf carregat");
        }
        
        return executorService.submit(() -> {
            try {
                // Configurar l'algoritme amb callbacks per actualitzacions de progrés
                currentAlgorithm.setProgressCallback((nodesExplored, nodesPruned, currentBound) -> {
                    notifyObservers(observer -> 
                        observer.onProgressUpdate(nodesExplored, nodesPruned, currentBound));
                });
                
                // Executar l'algoritme amb cancel·lació
                TSPSolution solution = currentAlgorithm.solve(currentGraph, withPruning, cancellationToken);
                this.lastSolution = solution;
                
                // Notificar solució trobada
                notifyObservers(observer -> observer.onSolutionFound(solution));

                NotificationService.Event event = new NotificationService.Event(NotificationService.EventType.INFO,
                    "Solució trobada: cost " + solution.getTotalCost() + 
                    ", nodes explorats: " + solution.getStatistics().getNodesExplored());
                notificationService.publishEvent(event);
                
            } catch (model.algorithm.CancellationToken.CancellationException e) {
                // Gestionar la cancel·lació de manera suau
                notifyObservers(observer -> observer.onComputationError("Càlcul cancel·lat per l'usuari"));

                NotificationService.Event event = new NotificationService.Event(NotificationService.EventType.WARNING,
                    "Execució cancel·lada per l'usuari");
                notificationService.publishEvent(event);
                
            } catch (Exception e) {
                notifyObservers(observer -> observer.onComputationError(e.getMessage()));

                NotificationService.Event event = new NotificationService.Event(NotificationService.EventType.ERROR,
                    "Error durant l'execució: " + e.getMessage());
                notificationService.publishEvent(event);
            }
        });
    }
    
    /**
     * Genera dades de visualització per al graf actual.
     */
    public void generateGraphVisualization() {
        if (currentGraph == null) {
            return;
        }
        
        executorService.submit(() -> {
            try {
                GraphVisualizationData visualizationData = 
                    currentGraph.generateVisualizationData(lastSolution);
                
                notifyObservers(observer -> 
                    observer.onVisualizationReady(visualizationData));
                
            } catch (Exception e) {
                notifyObservers(observer -> 
                    observer.onComputationError("Error generant visualització: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Exporta els resultats actuals a un fitxer CSV.
     * 
     * @param filePath ruta del fitxer on exportar
     * @throws IOException si hi ha problemes d'escriptura
     */
    public void exportResults(String filePath) throws IOException {
        if (lastSolution == null) {
            throw new IllegalStateException("No hi ha cap solució per exportar");
        }
        
        try (FileWriter writer = new FileWriter(filePath)) {
            // Capçalera CSV
            writer.write("Metric,Value\n");
            
            // Dades de la solució
            writer.write("Cost Total," + lastSolution.getTotalCost() + "\n");
            writer.write("Nodes Explorats," + lastSolution.getStatistics().getNodesExplored() + "\n");
            writer.write("Nodes Podats," + lastSolution.getStatistics().getNodesPruned() + "\n");
            writer.write("Temps Execucio (ms)," + lastSolution.getStatistics().getExecutionTime() + "\n");
            writer.write("Memoria Utilitzada (MB)," + lastSolution.getStatistics().getMemoryUsed() + "\n");
            
            // Ruta òptima
            writer.write("\nPath Optim\n");
            List<Integer> path = lastSolution.getPath();
            for (int i = 0; i < path.size(); i++) {
                writer.write("Passa " + (i + 1) + "," + path.get(i) + "\n");
            }
        }

        NotificationService.Event event = new  NotificationService.Event(NotificationService.EventType.INFO, "Resultats exportats a " + filePath);
        notificationService.publishEvent(event);
    }
    
    /**
     * Valida que una matriu d'adjacència sigui correcta per al TSP.
     * 
     * Comprova que la matriu no sigui null o buida, que tingui almenys 3 ciutats,
     * que sigui quadrada, que la diagonal sigui zero i que no hi hagi pesos negatius.
     * 
     * @param matrix matriu d'adjacència a validar
     * @throws IllegalArgumentException si la matriu no és vàlida
     */
    private void validateAdjacencyMatrix(double[][] matrix) {
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
            }
        }
    }
    
    // Mètodes per gestionar observadors del patró Observer
    /**
     * Registra un observador del model.
     *
     * @param observer objecte que serà notificat dels canvis
     */
    public void addObserver(ModelObserver observer) {
        observers.add(observer);
    }
    
    /**
     * Elimina un observador prèviament registrat.
     *
     * @param observer observador a eliminar
     */
    public void removeObserver(ModelObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notifica tots els observadors executant l'acció proporcionada.
     *
     * @param action acció a executar per a cada observador
     */
    private void notifyObservers(ObserverAction action) {
        for (ModelObserver observer : observers) {
            try {
                action.execute(observer);
            } catch (Exception e) {
                // En cas d'error amb un observador es continua amb la resta
            }
        }
    }
    
    @FunctionalInterface
    private interface ObserverAction {
        void execute(ModelObserver observer);
    }
    
    // Mètodes d'accés (getters) per obtenir l'estat del model
    /**
     * Retorna el graf actual carregat al model.
     *
     * @return graf utilitzat actualment
     */
    public Graph getCurrentGraph() {
        return currentGraph;
    }
    
    /**
     * Retorna la darrera solució calculada.
     *
     * @return objecte TSPSolution o null si encara no s'ha executat cap càlcul
     */
    public TSPSolution getLastSolution() {
        return lastSolution;
    }
    
    /**
     * Estableix l'algorisme a utilitzar per resoldre el TSP.
     * 
     * @param algorithmType tipus d'algorisme
     */
    public void setAlgorithm(AlgorithmType algorithmType) {
        if (algorithmType == null) {
            throw new IllegalArgumentException("El tipus d'algorisme no pot ser null");
        }
        
        this.algorithmType = algorithmType;
        this.currentAlgorithm = AlgorithmFactory.createAlgorithm(algorithmType);
        
        NotificationService.Event event = new NotificationService.Event(NotificationService.EventType.INFO,
            "Algorisme canviat a: " + algorithmType.getName());
        notificationService.publishEvent(event);
    }
    
    /**
     * Obté el tipus d'algorisme actual.
     * 
     * @return tipus d'algorisme actual
     */
    public AlgorithmType getCurrentAlgorithmType() {
        return algorithmType;
    }
    
    /**
     * Obté informació sobre tots els algorismes disponibles.
     * 
     * @return array de tipus d'algorisme disponibles
     */
    public AlgorithmType[] getAvailableAlgorithms() {
        return AlgorithmFactory.getAvailableAlgorithms();
    }
    
    /**
     * Interfície per observar canvis en el model.
     */
    public interface ModelObserver {
        void onGraphLoaded(Graph graph);
        void onSolutionFound(TSPSolution solution);
        void onVisualizationReady(GraphVisualizationData visualizationData);
        void onProgressUpdate(int nodesExplored, int nodesPruned, double currentBound);
        void onComputationError(String errorMessage);
    }
    
    /**
     * Allibera recursos quan l'aplicació es tanca.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}