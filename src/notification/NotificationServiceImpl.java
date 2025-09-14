package notification;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementació del servei de notificacions amb suport per processament asíncron.
 * 
 * Proporciona un sistema robust de publicació/subscripció amb:
 * - Processament asíncron d'esdeveniments
 * - Gestió segura de concurrència
 * - Logging d'errors
 * - Subscripció per tipus d'esdeveniment o global
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class NotificationServiceImpl implements NotificationService {
    
    private static final Logger logger = Logger.getLogger(NotificationServiceImpl.class.getName());
    
    // Mapa de listeners per tipus d'esdeveniment
    private final Map<EventType, List<EventListener>> listeners = new ConcurrentHashMap<>();
    
    // Listeners globals (reben tots els esdeveniments)
    private final List<EventListener> globalListeners = new CopyOnWriteArrayList<>();
    
    // Executor per processament asíncron
    private final ExecutorService executorService;
    
    // Flag per indicar si el servei està actiu
    private volatile boolean isRunning = true;
    
    // Estadístiques del servei
    private volatile long eventsPublished = 0;
    private volatile long eventsProcessed = 0;
    private volatile long processingErrors = 0;
    
    /**
     * Constructor que inicialitza el servei amb un pool de fils.
     */
    public NotificationServiceImpl() {
        this(2); // 2 fils per defecte
    }
    
    /**
     * Constructor que permet especificar el nombre de fils per processament.
     * 
     * @param numberOfThreads nombre de fils per al pool d'execució
     */
    public NotificationServiceImpl(int numberOfThreads) {
        if (numberOfThreads <= 0) {
            throw new IllegalArgumentException("El nombre de fils ha de ser positiu");
        }
        
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
        
        // Inicialitzar mapes per cada tipus d'esdeveniment
        for (EventType eventType : EventType.values()) {
            listeners.put(eventType, new CopyOnWriteArrayList<>());
        }
        
        logger.info("NotificationService inicialitzat amb " + numberOfThreads + " fils");
    }
    
    @Override
    public void publishEvent(Event event) {
        if (!isRunning) {
            logger.warning("Intent de publicar esdeveniment amb servei aturat: " + event);
            return;
        }
        
        if (event == null) {
            logger.warning("Intent de publicar esdeveniment null");
            return;
        }
        
        eventsPublished++;
        
        // Processar de manera asíncrona
        executorService.submit(() -> processEvent(event));
        
        logger.fine("Esdeveniment publicat: " + event);
    }
    
    @Override
    public void subscribe(EventType eventType, EventListener listener) {
        if (eventType == null || listener == null) {
            throw new IllegalArgumentException("EventType i EventListener no poden ser null");
        }
        
        List<EventListener> typeListeners = listeners.get(eventType);
        if (typeListeners != null && !typeListeners.contains(listener)) {
            typeListeners.add(listener);
            logger.fine("Listener subscrit a esdeveniments de tipus: " + eventType);
        }
    }
    
    @Override
    public void unsubscribe(EventType eventType, EventListener listener) {
        if (eventType == null || listener == null) {
            return;
        }
        
        List<EventListener> typeListeners = listeners.get(eventType);
        if (typeListeners != null) {
            typeListeners.remove(listener);
            logger.fine("Listener desubscrit d'esdeveniments de tipus: " + eventType);
        }
    }
    
    @Override
    public void subscribeToAll(EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("EventListener no pot ser null");
        }
        
        if (!globalListeners.contains(listener)) {
            globalListeners.add(listener);
            logger.fine("Listener subscrit a tots els esdeveniments");
        }
    }
    
    @Override
    public void unsubscribeFromAll(EventListener listener) {
        if (listener != null) {
            globalListeners.remove(listener);
            logger.fine("Listener desubscrit de tots els esdeveniments");
        }
    }
    
    @Override
    public void clearAllSubscriptions() {
        for (List<EventListener> typeListeners : listeners.values()) {
            typeListeners.clear();
        }
        globalListeners.clear();
        logger.info("Totes les subscripcions netejades");
    }
    
    @Override
    public boolean hasListeners(EventType eventType) {
        if (eventType == null) {
            return false;
        }
        
        List<EventListener> typeListeners = listeners.get(eventType);
        return (typeListeners != null && !typeListeners.isEmpty()) || !globalListeners.isEmpty();
    }
    
    @Override
    public int getListenerCount(EventType eventType) {
        if (eventType == null) {
            return 0;
        }
        
        List<EventListener> typeListeners = listeners.get(eventType);
        int typeCount = typeListeners != null ? typeListeners.size() : 0;
        return typeCount + globalListeners.size();
    }
    
    @Override
    public void shutdown() {
        logger.info("Aturant NotificationService...");
        isRunning = false;
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                logger.warning("El servei de notificacions ha estat forçat a aturar-se");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            logger.warning("Interrupció durant l'aturada del servei");
        }
        
        clearAllSubscriptions();
        logStatistics();
        logger.info("NotificationService aturat");
    }
    
    /**
     * Processa un esdeveniment enviant-lo a tots els listeners interessats.
     */
    private void processEvent(Event event) {
        if (!isRunning) {
            return;
        }
        
        List<EventListener> allListeners = new ArrayList<>();
        
        // Afegir listeners específics del tipus
        List<EventListener> typeListeners = listeners.get(event.getType());
        if (typeListeners != null) {
            allListeners.addAll(typeListeners);
        }
        
        // Afegir listeners globals
        allListeners.addAll(globalListeners);
        
        // Enviar esdeveniment a tots els listeners
        for (EventListener listener : allListeners) {
            try {
                listener.handleEvent(event);
                eventsProcessed++;
            } catch (Exception e) {
                processingErrors++;
                logger.log(Level.WARNING, "Error processant esdeveniment amb listener: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Registra estadístiques del servei.
     */
    private void logStatistics() {
        logger.info(String.format(
            "Estadístiques del NotificationService: " +
            "Esdeveniments publicats: %d, Processats: %d, Errors: %d",
            eventsPublished, eventsProcessed, processingErrors
        ));
    }
    
    /**
     * Obté estadístiques del servei.
     * 
     * @return estadístiques del servei
     */
    public ServiceStatistics getStatistics() {
        return new ServiceStatistics(eventsPublished, eventsProcessed, processingErrors, isRunning);
    }
    
    /**
     * Verifica si el servei està en funcionament.
     * 
     * @return true si el servei està actiu
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Obté informació sobre les subscripcions actuals.
     * 
     * @return mapa amb el recompte de listeners per tipus
     */
    public Map<EventType, Integer> getSubscriptionInfo() {
        Map<EventType, Integer> info = new HashMap<>();
        for (EventType eventType : EventType.values()) {
            info.put(eventType, getListenerCount(eventType));
        }
        return info;
    }
    
    /**
     * Classe interna amb estadístiques del servei.
     */
    public static class ServiceStatistics {
        private final long eventsPublished;
        private final long eventsProcessed;
        private final long processingErrors;
        private final boolean isRunning;
        
        public ServiceStatistics(long eventsPublished, long eventsProcessed, 
                               long processingErrors, boolean isRunning) {
            this.eventsPublished = eventsPublished;
            this.eventsProcessed = eventsProcessed;
            this.processingErrors = processingErrors;
            this.isRunning = isRunning;
        }
        
        public long getEventsPublished() { return eventsPublished; }
        public long getEventsProcessed() { return eventsProcessed; }
        public long getProcessingErrors() { return processingErrors; }
        public boolean isRunning() { return isRunning; }
        
        public double getErrorRate() {
            return eventsProcessed > 0 ? (double) processingErrors / eventsProcessed : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ServiceStatistics{publicats=%d, processats=%d, errors=%d, actiu=%s, %%errors=%.2f}",
                eventsPublished, eventsProcessed, processingErrors, isRunning, getErrorRate() * 100
            );
        }
    }
    
    /**
     * Interfície marcador per handlers de notificacions de la UI.
     * 
     * Els components de la UI poden implementar aquesta interfície per
     * rebre notificacions específiques de la interfície d'usuari.
     */
    public interface UINotificationHandler {
        /**
         * Gestiona una notificació de la interfície d'usuari.
         * 
         * @param event esdeveniment de notificació
         */
        void handleNotification(Event event);
    }
    
    /**
     * Subscriu un handler de la UI per rebre notificacions.
     * 
     * @param handler handler de la UI
     */
    public void subscribeUIHandler(UINotificationHandler handler) {
        if (handler != null) {
            subscribeToAll(handler::handleNotification);
        }
    }
    
    /**
     * Desubscriu un handler de la UI.
     * 
     * @param handler handler de la UI
     */
    public void unsubscribeUIHandler(UINotificationHandler handler) {
        if (handler != null) {
            unsubscribeFromAll(handler::handleNotification);
        }
    }
    
    /**
     * Mètode de compatibilitat per establir un handler de notificacions UI.
     * 
     * @param uiHandler handler de la interfície d'usuari
     */
    public void setUINotificationHandler(UINotificationHandler uiHandler) {
        subscribeUIHandler(uiHandler);
    }
    
    /**
     * Mètode de compatibilitat per netejar tots els listeners.
     */
    public void clearAllListeners() {
        clearAllSubscriptions();
    }
    
    /**
     * Obté el nombre total de listeners actius.
     * 
     * @return nombre total de listeners
     */
    public int getTotalListenerCount() {
        int total = globalListeners.size();
        for (List<EventListener> typeListeners : listeners.values()) {
            total += typeListeners.size();
        }
        return total;
    }
    
    /**
     * Mètode d'utilitat per crear esdeveniments d'informació ràpidament.
     * 
     * @param message missatge informatiu
     * @return nou esdeveniment d'informació
     */
    public static Event createInfoEvent(String message) {
        return new Event(EventType.INFO, message);
    }
    
    /**
     * Mètode d'utilitat per crear esdeveniments d'error ràpidament.
     * 
     * @param message missatge d'error
     * @return nou esdeveniment d'error
     */
    public static Event createErrorEvent(String message) {
        return new Event(EventType.ERROR, message);
    }
    
    /**
     * Mètode d'utilitat per crear esdeveniments d'advertència ràpidament.
     * 
     * @param message missatge d'advertència
     * @return nou esdeveniment d'advertència
     */
    public static Event createWarningEvent(String message) {
        return new Event(EventType.WARNING, message);
    }
}