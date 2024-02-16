package info.kgeorgiy.ja.konovalov.walk;

class UnknownHashingTypeException extends Exception {
    
    public UnknownHashingTypeException(String message) {
        super("Unknown hashing : " + message);
    }
}
