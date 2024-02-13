package info.kgeorgiy.ja.konovalov.walk;

public class FileHasher {
    static int hash(int hash, final byte[] data, final int len) {
        if (data == null) {
            return 0;
        }
        
        for (int i = 0; i < len; i++) {
            byte currentByte = data[i];
            
            // for negative bytes
            hash += currentByte & 0xff;
            hash += (hash << 10);
            hash ^= (hash >>> 6);
        }
        
        if (len == 0) {
            hash += hash << 3;
            hash ^= hash >>> 11;
            hash += hash << 15;
        }
        
        return hash;
    }
}
