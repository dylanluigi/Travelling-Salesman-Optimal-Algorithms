package model.algorithm;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementació senzilla de {@link CancellationToken} utilitzant un booleà atòmic.
 *
 * Token de cancel·lació segur per a fils que es pot compartir tant amb el fil de la UI
 * com amb el fil d'execució de l'algoritme.
 *
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class SimpleCancellationToken implements CancellationToken {
    
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    
    /**
     * Sol·licita la cancel·lació de l'algoritme.
     */
    public void cancel() {
        cancelled.set(true);
    }
    
    /**
     * Reinicia l'estat de cancel·lació.
     */
    public void reset() {
        cancelled.set(false);
    }
    
    @Override
    public boolean isCancellationRequested() {
        return cancelled.get();
    }
    
    /**
     * Comprova si s'ha cancel·lat i dorm breument per permetre actualitzacions de la UI.
     * Útil en bucles molt ajustats per evitar que la interfície es congeli.
     *
     * @throws CancellationException si es detecta la cancel·lació
     */
    public void checkCancellationAndYield() throws CancellationException {
        throwIfCancellationRequested();
        
        // Pausa breu per permetre actualitzar la interfície
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Algorithm interrupted");
        }
    }
}