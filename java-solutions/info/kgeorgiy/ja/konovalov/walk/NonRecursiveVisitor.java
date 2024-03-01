package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class NonRecursiveVisitor extends RecursiveVisitor {
    
    public NonRecursiveVisitor(final HashWriter writer, Hasher hasher) {
        super(writer, hasher);
    }
    
    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        super.writeZeroHash(dir);
        return FileVisitResult.SKIP_SUBTREE;
    }
}
