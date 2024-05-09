package info.kgeorgiy.ja.konovalov.hello;

import java.net.SocketException;

public class UncheckedSocketException extends AbstractUncheckedExceptionWrapper {
    UncheckedSocketException(Exception realException) {
        super(realException);
    }
}
