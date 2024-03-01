package info.kgeorgiy.ja.konovalov.walk;

public class ImpossibleToProcessFileException extends RuntimeException {
    ImpossibleToProcessFileException(String filename, String message) {
        super(String.format("Could not fully access file : : %s, the problem : %s", filename, message));
    }
}
