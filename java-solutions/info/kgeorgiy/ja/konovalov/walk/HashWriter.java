package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.io.Writer;

public class HashWriter {
    Writer writer;
    
    public HashWriter(Writer out) {
        writer = out;
    }
    
    public void writeHash(byte[] hash, String file) throws IOException {
        for (byte b : hash) {
            writer.write(String.format("%02x", b));
        }
        writer.write(String.format(" %s%n", file));
    }
    
    public void writeHash(final String hash, final String file) throws IOException {
        writer.write(String.format("%s %s%n", hash, file));
    }
}

