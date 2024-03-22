package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

public class UncheckedImplerException extends RuntimeException {
    ImplerException implerException;
    public UncheckedImplerException(final String message) {
        implerException = new ImplerException(message);
    }
    public ImplerException getImplerException() {
        return implerException;
    }
}
