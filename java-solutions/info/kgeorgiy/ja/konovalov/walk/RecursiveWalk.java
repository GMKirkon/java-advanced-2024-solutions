package info.kgeorgiy.ja.konovalov.walk;

public class RecursiveWalk {
    public static void main(final String... args) {
        try {
            WalkLauncher.launch(WalkModifications.RECURSIVE, args);
        } catch (final UnsupportedModificationError e) {
            System.err.println("Unknown modification was used" + WalkModifications.RECURSIVE);
        }
    }
}
