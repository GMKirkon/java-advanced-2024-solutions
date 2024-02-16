package info.kgeorgiy.ja.konovalov.walk;

public class UnsupportedModificationError extends Exception {
    public UnsupportedModificationError(String message) {
        super("Unsupported modification : " + message);
    }
}
