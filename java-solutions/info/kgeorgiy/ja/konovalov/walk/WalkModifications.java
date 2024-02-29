package info.kgeorgiy.ja.konovalov.walk;

import java.nio.file.FileVisitor;
import java.nio.file.Path;

public enum WalkModifications {
    RECURSIVE {
        @Override
        public FileVisitor<Path> createWalker(AbstractWriterAndHasher walker) {
            return new RecursiveVisitor(walker);
        }
    },
    NON_RECURSIVE {
        @Override
        public FileVisitor<Path> createWalker(AbstractWriterAndHasher walker) {
            return new NonRecursiveVisitor(walker);
        }
    };
    
    public abstract FileVisitor<Path> createWalker(AbstractWriterAndHasher walker);
}
