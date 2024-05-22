package info.kgeorgiy.ja.konovalov.bank;

public class Util {
    
    /** Utility class */
    private Util() {
    }
    /* package-private */ static void throwOrSuppress(boolean doesThrow, Exception e, String message) {
        if (doesThrow) {
            throw new RuntimeException(message + e.getMessage());
        } else {
            System.out.println(message + e.getMessage());
        }
    }
}
