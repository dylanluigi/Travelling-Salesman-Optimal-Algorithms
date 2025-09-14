package model.algorithm;

/**
 * Interfície de token de cancel·lació per controlar l'execució dels algorismes.
 *
 * Proporciona un mecanisme per cancel·lar de manera segura algorismes de llarga
 * durada. Els algorismes han de comprovar aquest token periòdicament i aturar-se
 * quan es demani la cancel·lació.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public interface CancellationToken {
    
    /**
     * Comprova si s'ha sol·licitat la cancel·lació.
     *
     * @return true si l'algorisme ha d'aturar l'execució
     */
    boolean isCancellationRequested();
    
    /**
     * Llença una {@link CancellationException} si s'ha sol·licitat cancel·lar.
     *
     * @throws CancellationException si s'ha demanat cancel·lar
     */
    default void throwIfCancellationRequested() throws CancellationException {
        if (isCancellationRequested()) {
            throw new CancellationException("Algorithm execution was cancelled");
        }
    }
    
    /**
     * Implementació senzilla que mai es cancela.
     */
    CancellationToken NONE = () -> false;
    
    /**
     * Excepció llançada quan un algorisme és cancel·lat.
     */
    class CancellationException extends RuntimeException {
        public CancellationException(String message) {
            super(message);
        }
    }
}