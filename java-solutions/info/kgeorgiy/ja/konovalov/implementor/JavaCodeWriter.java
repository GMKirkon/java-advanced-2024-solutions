package info.kgeorgiy.ja.konovalov.implementor;

import java.io.IOException;
import java.io.Writer;

/**
 * Helper class to write all .java files with implicitly writing unicode symbols to .java
 */
public class JavaCodeWriter extends Writer {
    
    /**
     * Actual writer used to output the result
     */
    Writer writer;
    
    
    /**
     * Creates JavaCodeWriter from a provided writer
     * @param writer actual writer to shadow
     */
    public JavaCodeWriter(Writer writer) {
        this.writer = writer;
    }
    
    /**
     * {@inheritDoc}
     * @param cbuf
     *         Array of characters
     *
     * @param off
     *         Offset from which to start writing characters
     *
     * @param len
     *         Number of characters to write
     *
     * @throws IOException if cannot output the result
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i = 0; i < len; ++i) {
            char current = cbuf[off + i];
            if (current >= 128) {
                writer.write(String.format("\\u%04X", (int)current));
            } else {
                writer.write(current);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     * @throws IOException in case you cannot flush
     */
    @Override
    public void flush() throws IOException {
        writer.flush();
    }
    
    /**
     * {@inheritDoc}
     * @throws IOException in case you cannot close
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }
}
