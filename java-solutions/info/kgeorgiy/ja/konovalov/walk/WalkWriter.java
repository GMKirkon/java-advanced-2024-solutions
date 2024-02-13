package info.kgeorgiy.ja.konovalov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

public class WalkWriter extends BufferedWriter {
    
    public WalkWriter(Writer out) {
        super(out);
    }
    
    public void writeHash(int hash, Path file) throws IOException {
        super.write(String.format("%08x ", hash) + file + System.lineSeparator());
    }
}

