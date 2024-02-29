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
    
    protected final AbstractWriterAndHasher walkHasher;
    
    public RecursiveVisitor(final AbstractWriterAndHasher walkHasher) {
        this.walkHasher = walkHasher;
    }
    
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        try (final InputStream in = new BufferedInputStream(newInputStream(file))) {
            int read;
            walkHasher.reset();
            while ((read = in.read(buff)) != -1) {
                walkHasher.hash(buff, read);
            }
            walkHasher.writeHash(walkHasher.getHash(), file.toString());
        } catch (final IOException e) {
            System.err.printf("Started processing file, but could not finish %s : %s", file, e.getMessage());
            walkHasher.writeZeroHash(file.toString());
        }
        return CONTINUE;
    }
    
    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        walkHasher.writeZeroHash(file.toString());
        return CONTINUE;
    }
}
