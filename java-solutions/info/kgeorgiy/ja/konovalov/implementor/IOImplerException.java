package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

/**
 * Represents an exception that occurs during IO operations in the {@link Implementor} class.
 * Extends the {@link ImplerException} class.
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
