package info.kgeorgiy.ja.konovalov.walk;

public class WalkingVisitException extends RuntimeException {
    WalkingVisitException(String message) {
        super("Could not fully access file : " + message);
    }
}
