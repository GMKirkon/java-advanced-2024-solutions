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


public class RecursiveWalker<T extends Path> extends SimpleFileVisitor<T> {
    
    private final byte[] buff = new byte[4096];
    private final WalkWriter writer;
    
    public RecursiveWalker(WalkWriter out) {
        writer = out;
    }
    
    @Override
    public FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws IOException {
        int hash = 0;
        try (
                final InputStream in = new BufferedInputStream(newInputStream(file))
        ) {
            int read = 0;
            while ((read = in.read(buff)) != -1) {
                hash = FileHasher.hash(hash, buff, read);
            }
            hash = FileHasher.hash(hash, buff, 0);
            
        } catch (final IOException e) {
            System.err.println("Started processing file, but could not finish" + file + " : " + e.getMessage());
            hash = 0;
        }
        writer.writeHash(hash, file);
        return CONTINUE;
    }
    
    @Override
    public FileVisitResult visitFileFailed(T file, IOException exc) throws IOException {
        System.err.println("Could not process file " + file + " : " + exc.getMessage());
        return CONTINUE;
    }
}
