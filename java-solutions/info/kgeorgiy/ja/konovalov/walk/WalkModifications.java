package info.kgeorgiy.ja.konovalov.walk;

import java.nio.file.FileVisitor;
import java.nio.file.Path;

public enum WalkModifications {
    RECURSIVE {
        @Override
        public FileVisitor<Path> createWalker(HashWriter walker, Hasher hasher) {
            return new RecursiveVisitor(walker, hasher);
        }
    },
    NON_RECURSIVE {
        @Override
        public FileVisitor<Path> createWalker(HashWriter walker, Hasher hasher) {
            return new NonRecursiveVisitor(walker, hasher);
        }
    };
    
    public abstract FileVisitor<Path> createWalker(HashWriter walker, Hasher hasher);
}
