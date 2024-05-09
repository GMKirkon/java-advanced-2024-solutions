package info.kgeorgiy.ja.konovalov.hello;

public class Internal {
    static Integer parsePositiveInteger(String arg, String errorMessage) {
        try {
            var result = Integer.parseInt(arg);
            if (result < 0) {
                System.err.printf("%s should be positive", errorMessage);
            }
            return result;
        } catch (NumberFormatException e) {
            System.err.printf("%s should be integer", errorMessage);
            return null;
        }
    }
    
    static boolean checkForPositive(int num, String errorMessage, boolean doesThrow) {
        if (num < 0) {
            if (doesThrow) {
                throw new IllegalStateException(errorMessage);
            } else {
                System.err.println(errorMessage);
                return false;
            }
        } else {
            return true;
        }
    }
}
