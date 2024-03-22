package info.kgeorgiy.ja.konovalov.walk;

public class RecursiveWalk {
    public static void main(final String... args) {
        WalkLauncher.launch(RecursiveVisitor::new, args);
    }
}
