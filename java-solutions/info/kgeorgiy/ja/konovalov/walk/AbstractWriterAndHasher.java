package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.io.Writer;

public abstract class AbstractWriterAndHasher {
    Writer writer;
    
    public AbstractWriterAndHasher(Writer out) {
        writer = out;
    }
    
    public void writeHash(byte[] hash, String file) throws IOException {
        for (byte b : hash) {
            writer.write(String.format("%02x", b));
        }
        writer.write(String.format(" %s%n", file));
    }
    
    public void writeZeroHash(final String file) throws IOException {
        writer.write(String.format("%s %s%n", "0".repeat(getHashLength()), file));
    }
    
    public abstract int getHashLength();
    public abstract byte[] getHash();
    
    public abstract void hash(final byte[] data, final int len);
    
    public abstract void reset();
}

