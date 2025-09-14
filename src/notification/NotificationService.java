package notification;

import java.util.EventObject;

/**
 * Servei de notificacions per gestionar la comunicació d'esdeveniments entre components.
 * 
 * Proporciona un sistema de publicació/subscripció per enviar notificacions
 * entre diferents parts de l'aplicació de manera desacoblada.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public interface NotificationService {
    
    /**
     * Publica un esdeveniment al sistema de notificacions.
     * 
     * @param event esdeveniment a publicar
     */
    void publishEvent(Event event);
    
    /**
     * Subscriu un listener per rebre esdeveniments d'un tipus específic.
     * 
     * @param eventType tipus d'esdeveniment
     * @param listener listener que processarà l'esdeveniment
     */
    void subscribe(EventType eventType, EventListener listener);
    
    /**
     * Desubscriu un listener d'un tipus d'esdeveniment.
     * 
     * @param eventType tipus d'esdeveniment
     * @param listener listener a desubscriure
     */
    void unsubscribe(EventType eventType, EventListener listener);
    
    /**
     * Subscriu un listener per rebre tots els esdeveniments.
     * 
     * @param listener listener que processarà tots els esdeveniments
     */
    void subscribeToAll(EventListener listener);
    
    /**
     * Desubscriu un listener de tots els esdeveniments.
     * 
     * @param listener listener a desubscriure
     */
    void unsubscribeFromAll(EventListener listener);
    
    /**
     * Neteja tots els listeners subscrits.
     */
    void clearAllSubscriptions();
    
    /**
     * Indica si hi ha listeners subscrits per a un tipus d'esdeveniment.
     * 
     * @param eventType tipus d'esdeveniment
     * @return true si hi ha listeners subscrits
     */
    boolean hasListeners(EventType eventType);
    
    /**
     * Obté el nombre de listeners subscrits per a un tipus d'esdeveniment.
     * 
     * @param eventType tipus d'esdeveniment
     * @return nombre de listeners subscrits
     */
    int getListenerCount(EventType eventType);
    
    /**
     * Allibera recursos i atura el servei de notificacions.
     */
    void shutdown();
    
    /**
     * Enumeració dels tipus d'esdeveniments suportats.
     */
    enum EventType {
        INFO,           // Informació general
        WARNING,        // Advertències
        ERROR,          // Errors
        PROGRESS,       // Actualitzacions de progrés
        GRAPH_LOADED,   // Graf carregat
        SOLUTION_FOUND, // Solució trobada
        COMPUTATION_ERROR, // Error de computació
        EXPORT_COMPLETED   // Exportació completada
    }
    
    /**
     * Classe que representa un esdeveniment del sistema.
     */
    class Event extends EventObject {
        private static final long serialVersionUID = 1L;
        
        private final EventType type;
        private final String message;
        private final Object data;
        private final long timestamp;
        
        /**
         * Constructor per esdeveniments amb missatge.
         * 
         * @param type tipus d'esdeveniment
         * @param message missatge associat
         */
        public Event(EventType type, String message) {
            this(type, message, null);
        }
        
        /**
         * Constructor complet per esdeveniments.
         * 
         * @param type tipus d'esdeveniment
         * @param message missatge associat
         * @param data dades addicionals
         */
        public Event(EventType type, String message, Object data) {
            super(new Object()); // Source fictícia
            this.type = type;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Obté el tipus d'esdeveniment.
         * 
         * @return tipus d'esdeveniment
         */
        public EventType getType() {
            return type;
        }
        
        /**
         * Obté el missatge de l'esdeveniment.
         * 
         * @return missatge
         */
        public String getMessage() {
            return message;
        }
        
        /**
         * Obté les dades addicionals de l'esdeveniment.
         * 
         * @return dades addicionals o null
         */
        public Object getData() {
            return data;
        }
        
        /**
         * Obté el timestamp de quan es va crear l'esdeveniment.
         * 
         * @return timestamp en mil·lisegons
         */
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * Obté les dades amb un tipus específic.
         * 
         * @param <T> tipus esperat de les dades
         * @param clazz classe del tipus esperat
         * @return dades amb el tipus especificat o null
         * @throws ClassCastException si les dades no són del tipus esperat
         */
        @SuppressWarnings("unchecked")
        public <T> T getData(Class<T> clazz) {
            if (data == null) {
                return null;
            }
            if (clazz.isInstance(data)) {
                return (T) data;
            }
            throw new ClassCastException("Les dades no són del tipus esperat: " + clazz.getName());
        }
        
        /**
         * Verifica si l'esdeveniment té dades del tipus especificat.
         * 
         * @param clazz classe del tipus a verificar
         * @return true si les dades són del tipus especificat
         */
        public boolean hasDataOfType(Class<?> clazz) {
            return data != null && clazz.isInstance(data);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Event{");
            sb.append("type=").append(type);
            sb.append(", message='").append(message).append('\'');
            sb.append(", timestamp=").append(timestamp);
            if (data != null) {
                sb.append(", data=").append(data.getClass().getSimpleName());
            }
            sb.append('}');
            return sb.toString();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            Event event = (Event) obj;
            return timestamp == event.timestamp &&
                   type == event.type &&
                   (message != null ? message.equals(event.message) : event.message == null);
        }
        
        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (message != null ? message.hashCode() : 0);
            result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
            return result;
        }
    }
    
    /**
     * Interfície per listeners d'esdeveniments.
     */
    @FunctionalInterface
    interface EventListener {
        /**
         * Processa un esdeveniment rebut.
         * 
         * @param event esdeveniment a processar
         */
        void handleEvent(Event event);
    }
}