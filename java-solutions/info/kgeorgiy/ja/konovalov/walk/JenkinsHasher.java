package info.kgeorgiy.ja.konovalov.walk;

public class JenkinsHasher implements Hasher {
    final String ERROR_HASH = "0".repeat(8);
    int hash = 0;
    
    @Override
    public byte[] getHash() {
        hash += hash << 3;
        hash ^= hash >>> 11;
        hash += hash << 15;
        // Ugly but... it is how it is.
        return new byte[] {
                (byte) (hash >>> 24),
                (byte) (hash >>> 16),
                (byte) (hash >>> 8),
                (byte) hash
        };
    }
    
    @Override
    public void hash(final byte[] data, final int len) {
        for (int i = 0; i < len; i++) {
            final byte currentByte = data[i];
            
            hash += currentByte & 0xff;
            hash += (hash << 10);
            hash ^= (hash >>> 6);
        }
    }
    
    @Override
    public void reset() {
        hash = 0;
    }
    
    @Override
    public String getErrorHash() {
        return ERROR_HASH;
    }
}
