package info.kgeorgiy.ja.konovalov.walk;

public class ImpossibleToOutputResult extends WalkingVisitException {
    ImpossibleToOutputResult(String filename, String message) {
        super(String.format("Could not output the hash for file : %s, the problem : %s", filename, message));
    }
}
