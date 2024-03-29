package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

/**
 * Helper exception to separate exception hierarchy.
 * Indicates that the problem in {@link Implementor} happened due to IO
 */
public class IOImplerException extends ImplerException {
    /**
     * Constructs IO Implementor Exception with a provided message
     * @param message error message
     */
    public IOImplerException(String message) {
        super("During IO happened problem " + message);
    }
}
