package info.kgeorgiy.ja.konovalov.walk;

public interface Hasher {
    byte[] getHash();
    
    void hash(final byte[] data, final int len);
    
    void reset();
    
    String getErrorHash();
}
