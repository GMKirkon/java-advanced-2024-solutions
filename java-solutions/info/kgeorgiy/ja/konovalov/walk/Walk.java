package info.kgeorgiy.ja.konovalov.walk;

public class Walk {
    public static void main(final String... args) {
        WalkLauncher.launch(NonRecursiveVisitor::new, args);
    }
}
