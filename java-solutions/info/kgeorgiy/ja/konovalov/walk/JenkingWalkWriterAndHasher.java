package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.io.Writer;

public class JenkingWalkWriterAndHasher extends AbstractWalkWriterAndHasher {
    
    
    public JenkingWalkWriterAndHasher(Writer out) {
        super(out);
    }
    
    int hash = 0;
    
    @Override
    public byte[] getHash() {
        hash += hash << 3;
        hash ^= hash >>> 11;
        hash += hash << 15;
        return java.nio.ByteBuffer.allocate(4).putInt(hash).array();
    }
    
    @Override
    public void hash(final byte[] data, final int len) {
        if (data == null) {
            return;
        }
        
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
    public void writeZeroHash(String file) throws IOException {
        super.writer.write(String.format("%08x ", 0) + file + System.lineSeparator());
    }
}
