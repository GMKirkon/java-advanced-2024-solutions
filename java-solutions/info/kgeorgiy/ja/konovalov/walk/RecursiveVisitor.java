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
    
    protected final AbstractWalkWriterAndHasher walkHasher;
    
    public RecursiveVisitor(final AbstractWalkWriterAndHasher walkHasher) {
        this.walkHasher = walkHasher;
    }
    
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        byte[] hash;
        try (
                final InputStream in = new BufferedInputStream(newInputStream(file))
        ) {
            int read = 0;
            walkHasher.reset();
            while ((read = in.read(buff)) != -1) {
                walkHasher.hash(buff, read);
            }
            hash = walkHasher.getHash();
        } catch (final IOException e) {
            System.err.println("Started processing file, but could not finish" + file + " : " + e.getMessage());
            walkHasher.writeZeroHash(file.toString());
            return CONTINUE;
        }
        
        walkHasher.writeHash(hash, file.toString());
        return CONTINUE;
    }
    
    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        walkHasher.writeZeroHash(file.toString());
        return CONTINUE;
    }
}
