package info.kgeorgiy.ja.konovalov.arrayset;

public class UncheckedUnmodifiableClassException extends UnsupportedOperationException {
    UncheckedUnmodifiableClassException() {
        super("Modifying methods are not allowed");
    }
}
