package info.kgeorgiy.ja.konovalov.walk;

public class ImpossibleToOpenFile extends RuntimeException {
    ImpossibleToOpenFile(String filename, String message) {
        super(String.format("Could not open the given file : %s, the problem : %s", filename, message));
    }
}
