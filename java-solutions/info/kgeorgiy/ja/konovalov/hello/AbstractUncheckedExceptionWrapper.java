package info.kgeorgiy.ja.konovalov.hello;

public class AbstractUncheckedExceptionWrapper extends RuntimeException {
    private final Exception realException;
    AbstractUncheckedExceptionWrapper(Exception realException) {
        this.realException = realException;
    }
    
    public Exception getRealException() {
        return realException;
    }
    
    @Override
    public String getMessage() {
        return realException.getMessage();
    }
}
