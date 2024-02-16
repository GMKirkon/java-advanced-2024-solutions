package info.kgeorgiy.ja.konovalov.walk;

public interface FileHasher {
    byte[] getHash();
    
    void hash(final byte[] data, final int len);
    
    void reset();
}
