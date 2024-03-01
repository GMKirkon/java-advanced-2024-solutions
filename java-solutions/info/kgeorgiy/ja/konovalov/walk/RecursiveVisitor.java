package info.kgeorgiy.ja.konovalov.walk;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.newInputStream;


public class RecursiveVisitor extends SimpleFileVisitor<Path> {
    private final byte[] buff = new byte[4096];
    
    protected final HashWriter writer;
    protected final Hasher hasher;
    
    public RecursiveVisitor(final HashWriter writer, Hasher hasher) {
        this.writer = writer;
        this.hasher = hasher;
    }
    
    
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        try (final InputStream in = new BufferedInputStream(newInputStream(file))) {
            int read;
            hasher.reset();
            while ((read = in.read(buff)) != -1) {
                hasher.hash(buff, read);
            }
            writer.writeHash(hasher.getHash(), file.toString());
        } catch (final IOException e) {
            System.err.printf("Started processing file, but could not finish %s : %s", file, e.getMessage());
            writeZeroHash(file);
        }
        return CONTINUE;
    }
    
    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        writeZeroHash(file);
        return CONTINUE;
    }
    
    public void writeZeroHash(final Path file) throws IOException {
        writer.writeHash(hasher.getErrorHash(), file.toString());
    }
}
