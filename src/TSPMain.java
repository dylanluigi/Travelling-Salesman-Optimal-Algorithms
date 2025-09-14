import controller.TSPController;
import model.TSPModel;
import notification.NotificationService;
import notification.NotificationServiceImpl;
import view.TSPView;

import javax.swing.*;
import javax.swing.UIManager;
import java.awt.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Classe principal de l'aplicació TSP (Problema del Viatjant de Comerç).
 * 
 * Aquesta aplicació implementa una solució completa al problema del viatjant de comerç
 * utilitzant l'algoritme Branch and Bound amb tècniques de poda i reducció de matrius.
 * 
 * Arquitectura MVC:
 * - Model: TSPModel - gestiona la lògica dels algoritmes i les dades.
 * - Vista: TSPView - interfície gràfica Swing
 * - Controlador: TSPController - coordina entre model i vista
 * 
 * Funcionalitats principals:
 * - Càrrega de grafs des de matriu d'adjacència
 * - Generació de grafs aleatoris
 * - Resolució amb Branch and Bound (amb/sense poda)
 * - Visualització del graf i solució òptima
 * - Estadístiques detallades del procés de poda
 * - Exportació de resultats
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 * @since 2025
 */
public class TSPMain {
    
    private static final Logger logger = Logger.getLogger(TSPMain.class.getName());
    
    private TSPModel model;
    private TSPView view;
    private TSPController controller;
    private NotificationService notificationService;
    
    /**
     * Inicialitza i executa l'aplicació TSP.
     */
    public void run() {
        // Configurar logging
        setupLogging();
        
        logger.info("Iniciant aplicació TSP...");
        
        // Mostrar splash screen
        showSplashScreen();
        
        // Inicialitzar components MVC
        initializeMVCComponents();
        
        // Iniciar la vista
        SwingUtilities.invokeLater(() -> {
            view.setVisible(true);
            logger.info("Aplicació TSP iniciada correctament");
        });
    }
    
    /**
     * Inicialitza els components seguint el patró MVC.
     */
    private void initializeMVCComponents() {
        try {
            // 1. Crear servei de notificacions
            notificationService = new NotificationServiceImpl();
            logger.info("Servei de notificacions creat");
            
            // 2. Crear model
            model = new TSPModel(notificationService);
            logger.info("Model TSP creat");
            
            // 3. Crear vista
            view = new TSPView();
            logger.info("Vista TSP creada");
            
            // 4. Crear controlador i connectar components
            controller = new TSPController(model, view, notificationService);
            logger.info("Controlador TSP creat i components connectats");
            
            // 5. Configurar notificacions UI
            if (notificationService instanceof NotificationServiceImpl) {
                ((NotificationServiceImpl) notificationService)
                    .setUINotificationHandler(view);
                logger.info("Handler de notificacions UI configurat");
            }
            
            // 6. Configurar tancament de l'aplicació
            view.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            view.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    shutdown();
                    System.exit(0);
                }
            });
            
        } catch (Exception e) {
            logger.severe("Error inicialitzant components MVC: " + e.getMessage());
            JOptionPane.showMessageDialog(null, 
                "Error inicialitzant l'aplicació: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    /**
     * Mostra una pantalla de càrrega mentre s'inicialitza l'aplicació.
     */
    private void showSplashScreen() {
        JWindow splash = new JWindow();
        splash.setLayout(new BorderLayout());
        
        // Panell principal amb gradient
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(30, 60, 114);
                Color color2 = new Color(42, 82, 152);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        // Títol
        JLabel title = new JLabel("Solucionador TSP");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Subtítol
        JLabel subtitle = new JLabel("Algoritme Ramificació i Poda");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(Color.LIGHT_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Barra de progrés
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setString("Carregant...");
        progressBar.setMaximumSize(new Dimension(200, 20));
        
        panel.add(Box.createVerticalGlue());
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(subtitle);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(progressBar);
        panel.add(Box.createVerticalGlue());
        
        splash.add(panel);
        splash.setSize(300, 200);
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);
        
        // Simular temps de càrrega
        Timer timer = new Timer(2000, e -> splash.dispose());
        timer.setRepeats(false);
        timer.start();
        
        try {
            Thread.sleep(2100); // Assegurar que el splash es vegi
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Configura el sistema de logging per a l'aplicació.
     */
    private void setupLogging() {
        try {
            // Crear directori de logs si no existeix
            java.io.File logsDir = new java.io.File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            
            // Configurar file handler
            FileHandler fileHandler = new FileHandler(
                "logs/tsp_app.log", 
                true // append
            );
            fileHandler.setFormatter(new SimpleFormatter());
            
            // Configurar logger principal
            Logger rootLogger = Logger.getLogger("");
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(java.util.logging.Level.INFO);
            
            logger.info("Sistema de logging configurat");
            
        } catch (Exception e) {
            System.err.println("Error configurant logging: " + e.getMessage());
        }
    }
    
    /**
     * Allibera recursos quan l'aplicació es tanca.
     */
    private void shutdown() {
        try {
            logger.info("Tancant aplicació TSP...");
            
            if (model != null) {
                model.shutdown();
                logger.info("Model TSP tancat");
            }
            
            if (notificationService != null) {
                notificationService.clearAllSubscriptions();
                logger.info("Servei de notificacions netejat");
            }
            
            logger.info("Aplicació tancada correctament");
            
        } catch (Exception e) {
            logger.severe("Error durant el tancament: " + e.getMessage());
        }
    }
    
    /**
     * Punt d'entrada principal de l'aplicació.
     * 
     * @param args arguments de la línia de comandes
     */
    public static void main(String[] args) {
        // Mostrar informació de la versió
        printApplicationInfo();
        
        try {
            // Crear i executar l'aplicació
            TSPMain app = new TSPMain();
            app.run();
            
        } catch (Exception e) {
            System.err.println("Error llançant l'aplicació: " + e.getMessage());
            e.printStackTrace();
            
            // Mostrar diàleg d'error
            JOptionPane.showMessageDialog(null, 
                "Error crític: " + e.getMessage() + "\n\nL'aplicació es tancarà.",
                "Error Fatal", JOptionPane.ERROR_MESSAGE);
            
            System.exit(1);
        }
    }
    
    /**
     * Mostra informació sobre l'aplicació.
     */
    private static void printApplicationInfo() {
        System.out.println("===========================================");
        System.out.println("    Problema del Viatjant de Comerç");
        System.out.println("    Branch and Bound amb Poda");
        System.out.println("    Versió 1.0 - 2025");
        System.out.println("===========================================");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + 
                          System.getProperty("os.version"));
        System.out.println("Available Processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Max Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
        System.out.println("===========================================");
        System.out.println();
    }
    
    /**
     * Obté informació sobre l'estat actual de l'aplicació.
     * 
     * @return informació de l'estat
     */
    public ApplicationInfo getApplicationInfo() {
        return new ApplicationInfo(
            model != null ? model.getCurrentGraph() != null : false,
            model != null ? model.getLastSolution() != null : false,
            notificationService != null ? 
                ((NotificationServiceImpl) notificationService).getTotalListenerCount() : 0
        );
    }
    
    /**
     * Classe per encapsular informació de l'estat de l'aplicació.
     */
    public static class ApplicationInfo {
        private final boolean hasGraph;
        private final boolean hasSolution;
        private final int activeListeners;
        private final long uptime;

        /**
         * Crea una nova instància amb l'estat indicat.
         *
         * @param hasGraph indica si hi ha graf carregat
         * @param hasSolution indica si s'ha calculat alguna solució
         * @param activeListeners nombre de listeners actius
         */
        public ApplicationInfo(boolean hasGraph, boolean hasSolution, int activeListeners) {
            this.hasGraph = hasGraph;
            this.hasSolution = hasSolution;
            this.activeListeners = activeListeners;
            this.uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        }

        /**
         * Indica si l'aplicació té un graf carregat.
         *
         * @return true si hi ha graf carregat
         */
        public boolean hasGraph() { return hasGraph; }

        /**
         * Indica si s'ha calculat una solució.
         *
         * @return true si hi ha solució disponible
         */
        public boolean hasSolution() { return hasSolution; }

        /**
         * Obté el nombre de listeners actius al servei de notificacions.
         *
         * @return nombre de listeners actius
         */
        public int getActiveListeners() { return activeListeners; }

        /**
         * Obté el temps d'execució de l'aplicació en mil·lisegons.
         *
         * @return temps d'execució en ms
         */
        public long getUptime() { return uptime; }
        
        @Override
        public String toString() {
            return String.format(
                "ApplicationInfo{hasGraph=%s, hasSolution=%s, listeners=%d, uptime=%d ms}",
                hasGraph, hasSolution, activeListeners, uptime
            );
        }
    }
}