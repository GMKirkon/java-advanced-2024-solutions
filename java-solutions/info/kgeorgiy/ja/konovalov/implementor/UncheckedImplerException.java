package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

/**
 * Unchecked version of ImplerException, used in order to work with streams without checked warnings.
 * Stores inside ImplerException that should be thrown when
 * UncheckedImplerException is captured at top-level of {@link Implementor}
 */
public class UncheckedImplerException extends RuntimeException {
    /**
     * Instance of actual exception that is to be thrown
     */
    private final ImplerException implerException;
    
    /**
     * Constructs UncheckedImplerException
     * @param message error message
     */
    public UncheckedImplerException(final String message) {
        implerException = new ImplerException(message);
    }
    
    /**
     * Gets actual stored ImplerException
     * @return actual ImplerException that should be thrown instead into the checked context
     */
    public ImplerException getImplerException() {
        return implerException;
    }
}
