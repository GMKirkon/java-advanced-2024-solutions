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
    protected final HashWriter writer;
    protected final Hasher hasher;
    private final byte[] buff = new byte[4096];
    
    public RecursiveVisitor(final HashWriter writer, final Hasher hasher) {
        this.writer = writer;
        this.hasher = hasher;
    }
    
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws ImpossibleToOutputResult {
        try (final InputStream in = new BufferedInputStream(newInputStream(file))) {
            hasher.reset();
            int read;
            while ((read = in.read(buff)) != -1) {
                hasher.hash(buff, read);
            }
            writer.writeHash(hasher.getHash(), file.toString());
        } catch (final IOException e) {
            writeZeroHash(file);
        }
        return CONTINUE;
    }
    
    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws ImpossibleToOutputResult {
        writeZeroHash(file);
        return CONTINUE;
    }
    
    public void writeZeroHash(final Path file) throws ImpossibleToOutputResult {
        writer.writeHash(hasher.getErrorHash(), file.toString());
    }
}
