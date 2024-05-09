package info.kgeorgiy.ja.konovalov.hello;

public class UncheckedClientQueryException extends AbstractUncheckedExceptionWrapper {
    
    UncheckedClientQueryException(Exception realException) {
        super(realException);
    }
}
