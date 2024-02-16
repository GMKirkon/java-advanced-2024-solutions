package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.io.Writer;

public abstract class AbstractWalkWriterAndHasher implements FileHasher {
    
    Writer writer;
    
    public AbstractWalkWriterAndHasher(java.io.Writer out) {
        writer = out;
    }
    
    public void writeHash(byte[] hash, String file) throws java.io.IOException {
        if (hash == null) {
            writeZeroHash(file);
            return;
        }
        for (byte b : hash) {
            writer.write(String.format("%02x", b));
        }
        writer.write(" " + file + System.lineSeparator());
    }
    
    public abstract void writeZeroHash(final String file) throws IOException;
}

