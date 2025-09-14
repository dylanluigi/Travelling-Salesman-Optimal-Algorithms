package view;

import controller.TSPController;
import model.algorithm.AlgorithmType;
import model.graph.Graph;
import model.graph.TSPSolution;
import model.graph.TSPStatistics;
import model.visualization.GraphVisualizationData;
import notification.NotificationService.Event;
import notification.NotificationServiceImpl.UINotificationHandler;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Interfície gràfica d'usuari per a l'aplicació TSP.
 * 
 * Proporciona una interfície Swing per interactuar amb l'aplicació del viatjant de comerç,
 * incloent entrada de dades, visualització del graf, controls d'execució i
 * mostrar de resultats i estadístiques.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class TSPView extends JFrame implements UINotificationHandler {
    
    private static final long serialVersionUID = 1L;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.00");
    
    // Components de la interfície
    private TSPController controller;
    private model.algorithm.SimpleCancellationToken cancellationToken;
    
    // Panells principals
    private JPanel mainPanel;
    private JPanel controlPanel;
    private JPanel visualizationPanel;
    private JPanel statusPanel;
    
    // Controls d'entrada
    private JSpinner citiesSpinner;
    private JTextField seedField;
    private JTextArea matrixTextArea;
    private JCheckBox pruningCheckBox;
    private JComboBox<AlgorithmType> algorithmComboBox;
    
    // Botons d'acció
    private JButton generateButton;
    private JButton loadMatrixButton;
    private JButton solveButton;
    private JButton stopButton;
    private JButton exportButton;
    private JButton visualizeButton;
    
    // Àrea de visualització
    private GraphCanvas graphCanvas;
    
    // Àrees d'informació
    private JTextArea solutionTextArea;
    private JTextArea statisticsTextArea;
    private JTextArea matrixDisplayArea;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    
    // Labels d'estadístiques
    private JLabel totalCostLabel;
    private JLabel nodesExploredLabel;
    private JLabel nodesPrunedLabel;
    private JLabel executionTimeLabel;
    
    /**
     * Constructor que inicialitza la interfície gràfica.
     */
    public TSPView() {
        cancellationToken = new model.algorithm.SimpleCancellationToken();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        configureWindow();
    }
    
    /**
     * Estableix el controlador associat a aquesta vista.
     * 
     * @param controller controlador TSP
     */
    public void setController(TSPController controller) {
        this.controller = controller;
    }
    
    /**
     * Inicialitza tots els components de la interfície.
     */
    private void initializeComponents() {
        // Controls d'entrada (crear primer)
        citiesSpinner = new JSpinner(new SpinnerNumberModel(5, 3, 50, 1));
        seedField = new JTextField("12345", 10);
        matrixTextArea = new JTextArea(8, 30);
        pruningCheckBox = new JCheckBox("Activar poda", true);
        algorithmComboBox = new JComboBox<>(AlgorithmType.values());
        algorithmComboBox.setSelectedItem(AlgorithmType.BRANCH_AND_BOUND); // Selecció predeterminada
        algorithmComboBox.setToolTipText("Selecciona l'algoritme per resoldre el TSP");
        
        // Canvas per visualització (crear abans dels panells)
        graphCanvas = new GraphCanvas();
        
        // Botons
        generateButton = new JButton("Generar Graf Aleatori");
        loadMatrixButton = new JButton("Carregar Matriu");
        solveButton = new JButton("Resoldre TSP");
        stopButton = new JButton("Aturar");
        exportButton = new JButton("Exportar Resultats");
        visualizeButton = new JButton("Visualitzar Graf");
        
        // Àrees d'informació
        solutionTextArea = new JTextArea(6, 40);
        statisticsTextArea = new JTextArea(8, 40);
        matrixDisplayArea = new JTextArea(10, 30);
        progressBar = new JProgressBar();
        statusLabel = new JLabel("Preparat per començar");
        
        // Labels d'estadístiques
        totalCostLabel = new JLabel("Cost total: --");
        nodesExploredLabel = new JLabel("Nodes explorats: --");
        nodesPrunedLabel = new JLabel("Nodes podats: --");
        executionTimeLabel = new JLabel("Temps d'execució: --");
        
        // Panells principals (crear després dels components individuals)
        mainPanel = new JPanel(new BorderLayout());
        controlPanel = createControlPanel();
        visualizationPanel = createVisualizationPanel();
        statusPanel = createStatusPanel();
        
        // Configurar components
        configureComponents();
    }
    
    /**
     * Configura les propietats dels components.
     */
    private void configureComponents() {
        // Text
        solutionTextArea.setEditable(false);
        solutionTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statisticsTextArea.setEditable(false);
        statisticsTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        matrixDisplayArea.setEditable(false);
        matrixDisplayArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        
        matrixTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        matrixTextArea.setText("Exemple:\n0 10 15 20\n10 0 35 25\n15 35 0 30\n20 25 30 0");
        
        // Botons inicials
        solveButton.setEnabled(false);
        stopButton.setEnabled(false);
        exportButton.setEnabled(false);
        visualizeButton.setEnabled(false);
        
        // Progress bar
        progressBar.setStringPainted(true);
        progressBar.setString("Preparat");
    }
    
    /**
     * Crea el panell de controls.
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        // Panell de generació
        JPanel generatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        generatePanel.setBorder(BorderFactory.createTitledBorder("Generar Graf"));
        generatePanel.add(new JLabel("Ciutats:"));
        generatePanel.add(citiesSpinner);
        generatePanel.add(new JLabel("Llavor:"));
        generatePanel.add(seedField);
        generatePanel.add(generateButton);
        
        // Panell de càrrega de matriu
        JPanel matrixPanel = new JPanel(new BorderLayout());
        matrixPanel.setBorder(BorderFactory.createTitledBorder("Carregar Matriu d'Adjacència"));
        matrixPanel.add(new JScrollPane(matrixTextArea), BorderLayout.CENTER);
        matrixPanel.add(loadMatrixButton, BorderLayout.SOUTH);
        
        // Panell d'algoritme
        JPanel algorithmPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        algorithmPanel.setBorder(BorderFactory.createTitledBorder("Selecció d'Algoritme"));
        algorithmPanel.add(new JLabel("Algoritme:"));
        algorithmPanel.add(algorithmComboBox);
        
        // Panell d'execució
        JPanel executePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        executePanel.setBorder(BorderFactory.createTitledBorder("Execució"));
        executePanel.add(pruningCheckBox);
        executePanel.add(solveButton);
        executePanel.add(stopButton);
        executePanel.add(visualizeButton);
        executePanel.add(exportButton);
        
        panel.add(generatePanel);
        panel.add(matrixPanel);
        panel.add(algorithmPanel);
        panel.add(executePanel);
        
        return panel;
    }
    
    /**
     * Crea el panell de visualització.
     */
    private JPanel createVisualizationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Visualització del Graf"));
        
        // Canvas de visualització
        JScrollPane canvasScrollPane = new JScrollPane(graphCanvas);
        canvasScrollPane.setPreferredSize(new Dimension(600, 400));
        
        panel.add(canvasScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Crea el panell de status i informació.
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Panell d'estadístiques ràpides
        JPanel quickStatsPanel = new JPanel(new GridLayout(2, 2));
        quickStatsPanel.setBorder(BorderFactory.createTitledBorder("Estadístiques"));
        quickStatsPanel.add(totalCostLabel);
        quickStatsPanel.add(nodesExploredLabel);
        quickStatsPanel.add(nodesPrunedLabel);
        quickStatsPanel.add(executionTimeLabel);
        
        // Panells de solució i estadístiques detallades
        JPanel solutionPanel = new JPanel(new BorderLayout());
        solutionPanel.setBorder(BorderFactory.createTitledBorder("Solució"));
        solutionPanel.add(new JScrollPane(solutionTextArea), BorderLayout.CENTER);
        
        JPanel detailedStatsPanel = new JPanel(new BorderLayout());
        detailedStatsPanel.setBorder(BorderFactory.createTitledBorder("Estadístiques Detallades"));
        detailedStatsPanel.add(new JScrollPane(statisticsTextArea), BorderLayout.CENTER);
        
        // Panell de matriu d'adjacència
        JPanel matrixPanel = new JPanel(new BorderLayout());
        matrixPanel.setBorder(BorderFactory.createTitledBorder("Matriu d'Adjacència"));
        matrixPanel.add(new JScrollPane(matrixDisplayArea), BorderLayout.CENTER);
        
        // Panell de status
        JPanel statusBarPanel = new JPanel(new BorderLayout());
        statusBarPanel.add(statusLabel, BorderLayout.WEST);
        statusBarPanel.add(progressBar, BorderLayout.CENTER);
        
        // Layout del panell principal
        JPanel infoPanel = new JPanel(new GridLayout(1, 3));
        infoPanel.add(solutionPanel);
        infoPanel.add(detailedStatsPanel);
        infoPanel.add(matrixPanel);
        
        panel.add(quickStatsPanel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(statusBarPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Configura el layout principal de la finestra.
     */
    private void setupLayout() {
        mainPanel.add(controlPanel, BorderLayout.WEST);
        mainPanel.add(visualizationPanel, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    /**
     * Configura els event handlers dels components.
     */
    private void setupEventHandlers() {
        generateButton.addActionListener(e -> generateRandomGraph());
        loadMatrixButton.addActionListener(e -> loadMatrixFromText());
        solveButton.addActionListener(e -> solveTSP());
        stopButton.addActionListener(e -> stopTSP());
        visualizeButton.addActionListener(e -> visualizeGraph());
        exportButton.addActionListener(e -> exportResults());
        
        // Event handler per mostrar descripció de l'algoritme
        algorithmComboBox.addActionListener(e -> {
            AlgorithmType selected = (AlgorithmType) algorithmComboBox.getSelectedItem();
            if (selected != null) {
                algorithmComboBox.setToolTipText(selected.getDescription());
            }
        });
        
        // Event handler per recomanar algoritme basat en nombre de ciutats
        citiesSpinner.addChangeListener(e -> {
            int cities = (Integer) citiesSpinner.getValue();
            AlgorithmType recommended = model.algorithm.AlgorithmFactory.recommendAlgorithm(cities);

            String baseTooltip = "Selecciona l'algoritme per resoldre el TSP";
            String recommendation = String.format(" (Recomanat per %d ciutats: %s)", cities, recommended.getName());
            algorithmComboBox.setToolTipText(baseTooltip + recommendation);

            AlgorithmType selected = (AlgorithmType) algorithmComboBox.getSelectedItem();
            if (selected != null) {
                int limit = model.algorithm.AlgorithmFactory.getPracticalLimit(selected);
                if (cities > limit && limit != Integer.MAX_VALUE) {
                    statusLabel.setText(" Algoritme " + selected.getName() + " pot ser lent per " + cities + " ciutats (límit recomanat: " + limit + ")");
                } else {
                    statusLabel.setText("Preparat per resoldre TSP amb " + cities + " ciutats");
                }
            }
        });
        
        // Establir tooltip inicial
        AlgorithmType initial = (AlgorithmType) algorithmComboBox.getSelectedItem();
        if (initial != null) {
            algorithmComboBox.setToolTipText(initial.getDescription());
        }
    }
    
    /**
     * Configura la finestra principal.
     */
    private void configureWindow() {
        setTitle("Problema del Viatjant de Comerç - Branch and Bound");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);
        pack();
    }
    
    // Mètodes d'acció
    
    private void generateRandomGraph() {
        try {
            int numCities = (Integer) citiesSpinner.getValue();
            long seed = Long.parseLong(seedField.getText().trim());
            
            if (controller != null) {
                controller.generateRandomGraph(numCities, seed);
                updateStatus("Graf aleatori generat amb " + numCities + " ciutats");
            }
        } catch (NumberFormatException e) {
            showError("Llavor no vàlida. Introduïu un número enter.");
        }
    }
    
    private void loadMatrixFromText() {
        try {
            String matrixText = matrixTextArea.getText().trim();
            double[][] matrix = parseMatrix(matrixText);
            
            if (controller != null) {
                controller.loadGraphFromMatrix(matrix);
                updateStatus("Matriu carregada correctament");
            }
        } catch (Exception e) {
            showError("Error parsejar la matriu: " + e.getMessage());
        }
    }
    
    private void solveTSP() {
        if (controller != null) {
            boolean withPruning = pruningCheckBox.isSelected();
            AlgorithmType selectedAlgorithm = (AlgorithmType) algorithmComboBox.getSelectedItem();
            
            // Reset token cancelacio
            cancellationToken.reset();
            
            // UI de computacio
            solveButton.setEnabled(false);
            stopButton.setEnabled(true);
            progressBar.setIndeterminate(true);
            progressBar.setString("Resolent TSP...");
            updateStatus("Executant algoritme " + selectedAlgorithm.getName() + "...");
            
            // Corre el algoritme de fons
            new Thread(() -> {
                try {
                    controller.solveTSPWithCancellation(selectedAlgorithm, withPruning, cancellationToken);
                    // La interfície es restablirà quan arribi la notificació de solució
                } catch (Exception e) {
                    // Restablir la interfície en cas d'error
                    SwingUtilities.invokeLater(() -> {
                        updateStatus("Error: " + e.getMessage());
                        resetUIAfterSolve();
                    });
                }
            }).start();
        }
    }
    
    private void stopTSP() {
        cancellationToken.cancel();
        updateStatus("Cancel·lant algoritme...");
        stopButton.setEnabled(false);
        
        // Restablir la interfície després d'un petit retard
        Timer timer = new Timer(1000, e -> resetUIAfterSolve());
        timer.setRepeats(false);
        timer.start();
    }
    
    private void resetUIAfterSolve() {
        solveButton.setEnabled(true);
        stopButton.setEnabled(false);
        progressBar.setIndeterminate(false);
        progressBar.setString("Completat");
    }
    
    private void visualizeGraph() {
        if (controller != null) {
            controller.requestGraphVisualization();
        }
    }
    
    private void exportResults() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filePath.endsWith(".csv")) {
                filePath += ".csv";
            }
            
            if (controller != null) {
                controller.exportResults(filePath);
            }
        }
    }
    
    // Mètodes públics per actualitzar la vista
    
    /**
     * Mostra un graf carregat.
     * 
     * @param graph graf a mostrar
     */
    public void displayGraph(Graph graph) {
        SwingUtilities.invokeLater(() -> {
            graphCanvas.setGraph(graph);
            displayAdjacencyMatrix(graph);
            updateStatus("Graf carregat: " + graph.getSize() + " ciutats");
        });
    }
    
    /**
     * Mostra la matriu d'adjacència del graf.
     * 
     * @param graph graf del qual mostrar la matriu
     */
    private void displayAdjacencyMatrix(Graph graph) {
        if (graph == null) {
            matrixDisplayArea.setText("No hi ha graf carregat");
            return;
        }
        
        double[][] matrix = graph.getAdjacencyMatrix();
        int n = matrix.length;
        StringBuilder sb = new StringBuilder();
        
        sb.append("Matriu d'Adjacència (").append(n).append("x").append(n).append("):\n");
        sb.append("════════════════════════════════════════\n\n");
        
        // Capçalera de columnes
        sb.append("     ");
        for (int j = 0; j < n; j++) {
            sb.append(String.format("%6d", j));
        }
        sb.append("\n");
        
        // Fila per fila
        for (int i = 0; i < n; i++) {
            sb.append(String.format("%3d: ", i));
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    sb.append("   -- ");
                } else {
                    sb.append(String.format("%6.1f", matrix[i][j]));
                }
            }
            sb.append("\n");
        }
        
        matrixDisplayArea.setText(sb.toString());
        matrixDisplayArea.setCaretPosition(0); // Scroll to top
    }
    
    /**
     * Mostra una solució TSP.
     * 
     * @param solution solució a mostrar
     */
    public void displaySolution(TSPSolution solution) {
        SwingUtilities.invokeLater(() -> {
            // Actualitzar àrea de text de la solució
            solutionTextArea.setText(formatSolution(solution));
            
            // Actualitzar canvas amb la solució
            graphCanvas.setSolution(solution);
            
            progressBar.setIndeterminate(false);
            progressBar.setValue(100);
            progressBar.setString("Completat");
            
            updateStatus("Solució trobada: cost " + DECIMAL_FORMAT.format(solution.getTotalCost()));
            
            // Activar botons
            exportButton.setEnabled(true);
            visualizeButton.setEnabled(true);
        });
    }
    
    /**
     * Actualitza les estadístiques mostrades.
     * 
     * @param statistics estadístiques a mostrar
     */
    public void updateStatistics(TSPStatistics statistics) {
        SwingUtilities.invokeLater(() -> {
            if (statistics != null) {
                // Labels ràpides - actualitzar amb valors reals
                totalCostLabel.setText("Cost total: " + DECIMAL_FORMAT.format(statistics.getBestCost()));
                nodesExploredLabel.setText("Nodes explorats: " + statistics.getNodesExplored());
                nodesPrunedLabel.setText("Nodes podats: " + statistics.getNodesPruned());
                executionTimeLabel.setText("Temps: " + statistics.getExecutionTime() + " ms");
                
                // Àrea detallada
                statisticsTextArea.setText(formatStatistics(statistics));
            } else {
                // Valors per defecte quan no hi ha estadístiques
                totalCostLabel.setText("Cost total: --");
                nodesExploredLabel.setText("Nodes explorats: --");
                nodesPrunedLabel.setText("Nodes podats: --");
                executionTimeLabel.setText("Temps: --");
                statisticsTextArea.setText("No hi ha estadístiques disponibles");
            }
        });
    }
    
    /**
     * Actualitza la visualització del graf.
     * 
     * @param visualizationData dades de visualització
     */
    public void updateGraphVisualization(GraphVisualizationData visualizationData) {
        SwingUtilities.invokeLater(() -> {
            graphCanvas.setVisualizationData(visualizationData);
            updateStatus("Visualització actualitzada");
        });
    }
    
    /**
     * Actualitza el progrés de l'execució.
     * 
     * @param nodesExplored nodes explorats
     * @param nodesPruned nodes podats
     * @param currentBound cota actual
     */
    public void updateProgress(int nodesExplored, int nodesPruned, double currentBound) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setString(String.format("Explorats: %d, Podats: %d", nodesExplored, nodesPruned));
            nodesExploredLabel.setText("Nodes explorats: " + nodesExplored);
            nodesPrunedLabel.setText("Nodes podats: " + nodesPruned);
        });
    }
    
    /**
     * Activa o desactiva el botó de resolució.
     * 
     * @param enabled si el botó ha d'estar activat
     */
    public void enableSolveButton(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            solveButton.setEnabled(enabled);
            visualizeButton.setEnabled(enabled);
        });
    }
    
    // Mètodes de notificació
    
    /**
     * Mostra un missatge d'informació.
     * 
     * @param message missatge a mostrar
     */
    public void showInfo(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Informació", JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    /**
     * Mostra un missatge d'advertència.
     * 
     * @param message missatge a mostrar
     */
    public void showWarning(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Advertència", JOptionPane.WARNING_MESSAGE);
        });
    }
    
    /**
     * Mostra un missatge d'error.
     * 
     * @param message missatge a mostrar
     */
    public void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
            progressBar.setIndeterminate(false);
            progressBar.setString("Error");
            updateStatus("Error: " + message);
        });
    }
    
    // Mètodes d'utilitat
    
    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
        });
    }
    
    private double[][] parseMatrix(String matrixText) {
        String[] lines = matrixText.split("\n");
        int size = lines.length;
        double[][] matrix = new double[size][size];
        
        for (int i = 0; i < size; i++) {
            String[] values = lines[i].trim().split("\\s+");
            if (values.length != size) {
                throw new IllegalArgumentException("La matriu ha de ser quadrada");
            }
            
            for (int j = 0; j < size; j++) {
                matrix[i][j] = Double.parseDouble(values[j]);
            }
        }
        
        return matrix;
    }
    
    private String formatSolution(TSPSolution solution) {
        StringBuilder sb = new StringBuilder();
        sb.append("SOLUCIÓ TSP\n");
        sb.append("===========\n\n");
        sb.append("Camí òptim:\n");
        sb.append(solution.getPathAsString()).append("\n\n");
        sb.append("Cost total: ").append(DECIMAL_FORMAT.format(solution.getTotalCost())).append("\n");
        sb.append("Nombre de ciutats: ").append(solution.getNumberOfCities()).append("\n");
        sb.append("Cost mitjà per aresta: ").append(DECIMAL_FORMAT.format(solution.getAverageCostPerEdge()));
        
        return sb.toString();
    }
    
    private String formatStatistics(TSPStatistics statistics) {
        StringBuilder sb = new StringBuilder();
        sb.append("ESTADÍSTIQUES D'EXECUCIÓ\n");
        sb.append("========================\n\n");
        sb.append("Nodes explorats: ").append(statistics.getNodesExplored()).append("\n");
        sb.append("Nodes podats: ").append(statistics.getNodesPruned()).append("\n");
        sb.append("Total nodes generats: ").append(statistics.getTotalNodesGenerated()).append("\n");
        sb.append("Percentatge de poda: ").append(DECIMAL_FORMAT.format(statistics.getPruningPercentage())).append("%\n\n");
        sb.append("Temps d'execució: ").append(statistics.getExecutionTime()).append(" ms\n");
        sb.append("Memòria utilitzada: ").append(DECIMAL_FORMAT.format(statistics.getMemoryUsed())).append(" MB\n");
        sb.append("Nodes per segon: ").append(DECIMAL_FORMAT.format(statistics.getNodesPerSecond()));
        
        return sb.toString();
    }
    
    // Implementació de UINotificationHandler
    @Override
    public void handleNotification(Event event) {
        switch (event.getType()) {
            case INFO:
                showInfo(event.getMessage());
                // If the message indicates solution found, reset UI
                if (event.getMessage().contains("Solució trobada")) {
                    SwingUtilities.invokeLater(() -> {
                        resetUIAfterSolve();
                        updateStatus("Solució trobada");
                    });
                }
                break;
            case WARNING:
                showWarning(event.getMessage());
                // If the message indicates cancellation, reset UI
                if (event.getMessage().contains("cancel·lada")) {
                    SwingUtilities.invokeLater(() -> {
                        resetUIAfterSolve();
                        updateStatus("Execució cancel·lada");
                    });
                }
                break;
            case ERROR:
                showError(event.getMessage());
                // També restablir la interfície en cas d'error
                SwingUtilities.invokeLater(() -> {
                    resetUIAfterSolve();
                });
                break;
            default:
                // Ignorar altres tipus d'events
                break;
        }
    }
    
    /**
     * Canvas personalitzat per dibuixar el graf.
     */
    private class GraphCanvas extends JPanel {
        private static final long serialVersionUID = 1L;
        
        private Graph graph;
        private TSPSolution solution;
        private GraphVisualizationData visualizationData;
        
        public GraphCanvas() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(600, 400));
        }
        
        public void setGraph(Graph graph) {
            this.graph = graph;
            this.solution = null;
            this.visualizationData = null;
            repaint();
        }
        
        public void setSolution(TSPSolution solution) {
            this.solution = solution;
            repaint();
        }
        
        public void setVisualizationData(GraphVisualizationData data) {
            this.visualizationData = data;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (visualizationData != null) {
                drawVisualizationData(g2d);
            } else if (graph != null) {
                drawSimpleGraph(g2d);
            } else {
                drawPlaceholder(g2d);
            }
        }
        
        private void drawVisualizationData(Graphics2D g2d) {
            // Implementar dibuix amb dades de visualització
            List<GraphVisualizationData.City> cities = visualizationData.getCities();
            List<GraphVisualizationData.Edge> edges = visualizationData.getEdges();
            
            // Dibuixar arestes
            g2d.setStroke(new BasicStroke(1));
            for (GraphVisualizationData.Edge edge : edges) {
                GraphVisualizationData.City from = cities.get(edge.getFromCity());
                GraphVisualizationData.City to = cities.get(edge.getToCity());
                
                if (edge.isOptimal()) {
                    g2d.setColor(Color.RED);
                    g2d.setStroke(new BasicStroke(3));
                } else {
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.setStroke(new BasicStroke(1));
                }
                
                g2d.drawLine((int)from.getX(), (int)from.getY(), 
                           (int)to.getX(), (int)to.getY());
            }
            
            // Dibuixar ciutats
            for (GraphVisualizationData.City city : cities) {
                int x = (int)city.getX() - 10;
                int y = (int)city.getY() - 10;
                
                g2d.setColor(city.isStartCity() ? Color.GREEN : Color.BLUE);
                g2d.fillOval(x, y, 20, 20);
                g2d.setColor(Color.red);
                g2d.setColor(Color.BLACK);
                g2d.drawString(String.valueOf(city.getIndex()), 
                             (int)city.getX() - 5, (int)city.getY() + 5);
            }
        }
        
        private void drawSimpleGraph(Graphics2D g2d) {
            // Dibuix simple quan només tenim el graf
            int n = graph.getSize();
            double centerX = getWidth() / 2.0;
            double centerY = getHeight() / 2.0;
            double radius = Math.min(getWidth(), getHeight()) / 3.0;

            // Calcular posicions circulars
            Point[] positions = new Point[n];
            for (int i = 0; i < n; i++) {
                double angle = 2 * Math.PI * i / n;
                int x = (int)(centerX + radius * Math.cos(angle));
                int y = (int)(centerY + radius * Math.sin(angle));
                positions[i] = new Point(x, y);
            }
            
            // Dibuixar arestes
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    g2d.drawLine(positions[i].x, positions[i].y, 
                               positions[j].x, positions[j].y);
                }
            }
            
            // Dibuixar camí òptim si hi ha solució
            if (solution != null) {
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(3));
                List<Integer> path = solution.getPath();
                for (int i = 0; i < path.size() - 1; i++) {
                    Point from = positions[path.get(i)];
                    Point to = positions[path.get(i + 1)];
                    g2d.drawLine(from.x, from.y, to.x, to.y);
                }
                // Tancar el cicle
                if (path.size() > 2) {
                    Point last = positions[path.get(path.size() - 1)];
                    Point first = positions[path.get(0)];
                    g2d.drawLine(last.x, last.y, first.x, first.y);
                }
            }
            
            // Dibuixar ciutats
            for (int i = 0; i < n; i++) {
                g2d.setColor(i == 0 ? Color.GREEN : Color.BLUE);
                g2d.fillOval(positions[i].x - 10, positions[i].y - 10, 20, 20);
                
                g2d.setColor(Color.BLACK);
                g2d.drawString(String.valueOf(i), positions[i].x - 5, positions[i].y + 5);
            }
        }
        
        private void drawPlaceholder(Graphics2D g2d) {
            g2d.setColor(Color.GRAY);
            String message = "Genereu o carregueu un graf per veure la visualització";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2;
            g2d.drawString(message, x, y);
        }
    }
}