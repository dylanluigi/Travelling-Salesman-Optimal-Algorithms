package controller;

import model.TSPModel;
import model.algorithm.AlgorithmType;
import model.graph.Graph;
import model.graph.TSPSolution;
import model.visualization.GraphVisualizationData;
import notification.NotificationService;
import notification.NotificationService.Event;
import notification.NotificationService.EventType;
import notification.NotificationServiceImpl.UINotificationHandler;
import view.TSPView;

import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Controlador principal per a l'aplicació TSP que gestiona la lògica
 * i coordina la comunicació entre el model (TSPModel) i la vista (TSPView).
 * 
 * Aquest controlador s'encarrega de:
 * - Gestionar la càrrega i generació de grafs per al problema TSP
 * - Coordinar l'execució de l'algoritme Branch and Bound amb poda
 * - Manejar visualitzacions del graf i de la solució òptima
 * - Gestionar estadístiques de poda (nodes explorats, descartats, cotes)
 * - Controlar l'exportació de resultats
 * - Gestionar notificacions i esdeveniments del sistema
 * 
 * Implementa els patrons Observer per rebre esdeveniments del model i
 * UINotificationHandler per gestionar notificacions de la interfície d'usuari.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 * @since 1.0
 */
public class TSPController implements TSPModel.ModelObserver, UINotificationHandler {
    
    private static final Logger logger = Logger.getLogger(TSPController.class.getName());
    
    private TSPModel model;
    private TSPView view;
    private NotificationService notificationService;
    
    private Future<?> currentTask;
    
    /**
     * Constructor que inicialitza el controlador amb referències al model i vista.
     * 
     * @param model el model TSP que conté la lògica y algoritmes
     * @param view la vista que gestiona la interfície d'usuari
     * @param notificationService servei per gestionar notificacions
     */
    public TSPController(TSPModel model, TSPView view, NotificationService notificationService) {
        this.model = model;
        this.view = view;
        this.notificationService = notificationService;
        
        // Registrar com a observador del model
        model.addObserver(this);
        
        // Configurar el controlador a la vista
        view.setController(this);
    }
    
    /**
     * Carrega un graf des d'una matriu d'adjacència proporcionada per l'usuari.
     * 
     * @param adjacencyMatrix matriu d'adjacència que representa el graf
     */
    public void loadGraphFromMatrix(double[][] adjacencyMatrix) {
        try {
            model.loadGraphFromMatrix(adjacencyMatrix);
            logger.info("Graf carregat correctament des de matriu d'adjacència");
        } catch (Exception e) {
            logger.severe("Error carregant graf: " + e.getMessage());
            notifyError(ErrorCode.GRAPH_LOAD_ERROR, "Error carregant el graf: " + e.getMessage());
        }
    }
    
    /**
     * Genera un graf aleatori amb el nombre especificat de ciutats.
     * 
     * @param numCities nombre de ciutats del graf
     * @param seed llavor per a la generació aleatòria (per reproducibilitat)
     */
    public void generateRandomGraph(int numCities, long seed) {
        try {
            model.generateRandomGraph(numCities, seed);
            logger.info("Graf aleatori generat amb " + numCities + " ciutats");
        } catch (Exception e) {
            logger.severe("Error generant graf aleatori: " + e.getMessage());
            notifyError(ErrorCode.GRAPH_GENERATION_ERROR, "Error generant graf aleatori: " + e.getMessage());
        }
    }
    
    /**
     * Executa l'algoritme seleccionat per trobar la solució òptima al TSP.
     * 
     * @param algorithmType el tipus d'algoritme a utilitzar
     * @param withPruning si s'ha d'aplicar poda o no
     */
    public void solveTSP(AlgorithmType algorithmType, boolean withPruning) {
        if (model.getCurrentGraph() == null) {
            notifyError(ErrorCode.NO_GRAPH_LOADED, "No hi ha cap graf carregat");
            return;
        }
        
        try {
            // Cancel·lar tasca anterior si existeix
            if (currentTask != null && !currentTask.isDone()) {
                currentTask.cancel(true);
            }
            
            // Establir l'algoritme al model
            model.setAlgorithm(algorithmType);
            
            currentTask = model.solveTSPAsync(withPruning);
            logger.info("Iniciada resolució TSP amb algoritme: " + algorithmType.getName() + ", poda: " + withPruning);
        } catch (Exception e) {
            logger.severe("Error executant TSP: " + e.getMessage());
            notifyError(ErrorCode.ALGORITHM_ERROR, "Error executant l'algoritme: " + e.getMessage());
        }
    }
    
    /**
     * Resol el TSP amb suport de cancel·lació.
     *
     * @param algorithmType tipus d'algorisme a utilitzar
     * @param withPruning indica si s'ha d'aplicar poda
     * @param cancellationToken token per cancel·lar l'operació
     */
    public void solveTSPWithCancellation(AlgorithmType algorithmType, boolean withPruning, 
                                        model.algorithm.CancellationToken cancellationToken) {
        if (model.getCurrentGraph() == null) {
            notifyError(ErrorCode.NO_GRAPH_LOADED, "No hi ha cap graf carregat");
            return;
        }
        
        try {
            // Cancel·lar tasca anterior si existeix
            if (currentTask != null && !currentTask.isDone()) {
                currentTask.cancel(true);
            }
            
            // Establir l'algorisme i resoldre amb cancel·lació
            model.setAlgorithm(algorithmType);
            currentTask = model.solveTSPAsyncWithCancellation(withPruning, cancellationToken);
            logger.info("Iniciada resolució TSP amb suport de cancel·lació: " + algorithmType.getName());
        } catch (Exception e) {
            logger.severe("Error executant TSP amb cancel·lació: " + e.getMessage());
            notifyError(ErrorCode.ALGORITHM_ERROR, "Error executant l'algoritme: " + e.getMessage());
        }
    }
    
    /**
     * Sol·licita la visualització del graf actual.
     */
    public void requestGraphVisualization() {
        if (model.getCurrentGraph() != null) {
            model.generateGraphVisualization();
        }
    }
    
    /**
     * Exporta els resultats actuals a un fitxer CSV.
     * 
     * @param filePath ruta del fitxer on exportar
     */
    public void exportResults(String filePath) {
        try {
            model.exportResults(filePath);
            logger.info("Resultats exportats a: " + filePath);
        } catch (Exception e) {
            logger.severe("Error exportant resultats: " + e.getMessage());
            notifyError(ErrorCode.EXPORT_ERROR, "Error exportant resultats: " + e.getMessage());
        }
    }
    
    // Implementació de ModelObserver

    /**
     * S'executa quan el model ha carregat un graf correctament.
     *
     * @param graph graf carregat
     */
    @Override
    public void onGraphLoaded(Graph graph) {
        view.displayGraph(graph);
        view.enableSolveButton(true);
    }

    /**
     * S'executa quan el model troba una solució al problema.
     *
     * @param solution solució trobada
     */
    @Override
    public void onSolutionFound(TSPSolution solution) {
        view.displaySolution(solution);
        view.updateStatistics(solution.getStatistics());
    }

    /**
     * S'executa quan es generen dades de visualització per al graf.
     *
     * @param visualizationData dades de visualització del graf
     */
    @Override
    public void onVisualizationReady(GraphVisualizationData visualizationData) {
        view.updateGraphVisualization(visualizationData);
    }

    /**
     * Actualitza la informació de progrés durant l'execució de l'algoritme.
     *
     * @param nodesExplored nodes explorats fins ara
     * @param nodesPruned nodes podats fins ara
     * @param currentBound cota actual
     */
    @Override
    public void onProgressUpdate(int nodesExplored, int nodesPruned, double currentBound) {
        view.updateProgress(nodesExplored, nodesPruned, currentBound);
    }

    /**
     * S'executa quan es produeix un error durant el càlcul del TSP.
     *
     * @param errorMessage missatge d'error
     */
    @Override
    public void onComputationError(String errorMessage) {
        notifyError(ErrorCode.ALGORITHM_ERROR, errorMessage);
    }

    // Implementació de UINotificationHandler

    /**
     * Gestiona una notificació rebuda de la interfície d'usuari.
     *
     * @param event esdeveniment de notificació
     */
    @Override
    public void handleNotification(Event event) {
        // Gestionar notificacions de la interfície d'usuari
        switch (event.getType()) {
            case INFO:
                view.showInfo(event.getMessage());
                break;
            case WARNING:
                view.showWarning(event.getMessage());
                break;
            case ERROR:
                view.showError(event.getMessage());
                break;
        }
    }
    
    /**
     * Notifica un error al sistema de notificacions.
     *
     * @param errorCode codi d'error
     * @param message missatge descriptiu
     */
    private void notifyError(ErrorCode errorCode, String message) {
        Event errorEvent = new Event(EventType.ERROR, message);
        notificationService.publishEvent(errorEvent);
    }
    
    /**
     * Codis d'error per a l'aplicació TSP.
     */
    public enum ErrorCode {
        GRAPH_LOAD_ERROR,
        GRAPH_GENERATION_ERROR,
        NO_GRAPH_LOADED,
        ALGORITHM_ERROR,
        EXPORT_ERROR
    }
}