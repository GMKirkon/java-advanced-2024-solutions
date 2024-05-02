package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.io.Writer;
import java.util.HexFormat;

public class HashWriter {
    Writer writer;
    
    public HashWriter(Writer out) {
        writer = out;
    }
    
    public void writeHash(byte[] hash, String file) throws ImpossibleToOutputResult {
        writeHash(HexFormat.of().formatHex(hash), file);
    }
    
    public void writeHash(final String hash, final String file) throws ImpossibleToOutputResult {
        try {
            writer.write(String.format("%s %s%n", hash, file));
        } catch (IOException | SecurityException e) {
            throw new ImpossibleToOutputResult(file, e.getMessage());
        }
    }
}

