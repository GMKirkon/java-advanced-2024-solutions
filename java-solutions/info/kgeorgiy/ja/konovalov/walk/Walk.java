package info.kgeorgiy.ja.konovalov.walk;

public class Walk {
    public static void main(final String... args) {
        try {
            WalkLauncher.launch(WalkModifications.NON_RECURSIVE, args);
        } catch (UnsupportedModificationError e) {
            System.err.println("Unknown modification was used" + WalkModifications.NON_RECURSIVE);
        }
    }
}
