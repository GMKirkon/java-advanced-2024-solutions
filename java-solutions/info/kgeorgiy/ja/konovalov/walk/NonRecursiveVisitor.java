package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class NonRecursiveVisitor extends RecursiveVisitor {
    
    public NonRecursiveVisitor(final AbstractWalkWriterAndHasher writer) {
        super(writer);
    }
    
    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        super.walkHasher.writeZeroHash(dir.toString());
        return FileVisitResult.SKIP_SUBTREE;
    }
}
