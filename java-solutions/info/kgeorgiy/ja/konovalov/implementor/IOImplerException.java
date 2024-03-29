package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

public class IOImplerException extends ImplerException {
    public IOImplerException(String message) {
        super("During IO happened problem " + message);
    }
}
