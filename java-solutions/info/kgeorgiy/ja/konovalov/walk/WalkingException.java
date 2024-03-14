package info.kgeorgiy.ja.konovalov.walk;

public class WalkingException extends RuntimeException {
    WalkingException(String message) {
        super("In the walk invocation : " + message);
    }
}
