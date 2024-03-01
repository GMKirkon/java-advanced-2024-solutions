package info.kgeorgiy.ja.konovalov.walk;

public class JenkinsHasher implements Hasher {
    int hash = 0;
    final String ERROR_HASH = "0".repeat(8);
    
    @Override
    public byte[] getHash() {
        hash += hash << 3;
        hash ^= hash >>> 11;
        hash += hash << 15;
        return java.nio.ByteBuffer.allocate(4).putInt(hash).array();
    }
    
    @Override
    public void hash(final byte[] data, final int len) {
        for (int i = 0; i < len; i++) {
            byte currentByte = data[i];
            
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
