package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.*;

public class WalkLauncher {
    public static void launch(WalkModifications modificationType, final String... args) throws UnsupportedModificationError {
        if (args == null || args.length < 2 || args.length > 3) {
            printUsagePattern();
            return;
        }
        
        final HashingType hashingType;
        
        for (String arg : args) {
            if (arg == null) {
                printUsagePattern();
                return;
            }
        }
        
        if (args.length == 2) {
            hashingType = HashingType.JENKINS;
        } else {
            switch (args[2]) {
                case "sha-1" -> hashingType = HashingType.SHA_1;
                case "jenkins" -> hashingType = HashingType.JENKINS;
                case null, default -> {
                    printUsagePattern();
                    return;
                }
            }
        }
        
        final Path inputFile;
        final Path outputFile;
        try {
            inputFile = Path.of(args[0]);
            outputFile = Path.of(args[1]);
        } catch (InvalidPathException e) {
            System.err.println("Provided input or output file was not correct: " + e.getMessage());
            return;
        }
        
        final Path parent = outputFile.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                System.err.println("could not create directories for outputFile path");
            }
        }
        
        try (
                final var in = newBufferedReader(inputFile, UTF_8);
                final var out = newBufferedWriter(outputFile, UTF_8)
        ) {
            final AbstractWalkWriterAndHasher hasherAndWalker;
            switch (hashingType) {
                case SHA_1 -> {
                    try {
                        hasherAndWalker = new info.kgeorgiy.ja.konovalov.walk.Sha1WalkWriterAndHasher(out);
                    } catch (NoSuchAlgorithmException e) {
                        System.err.println("Suddenly Java does not support SHA-1");
                        return;
                    }
                }
                case JENKINS -> hasherAndWalker = new JenkingWalkWriterAndHasher(out);
                case null, default -> {
                    System.err.println("Unsupported hashing type was chosen");
                    return;
                }
            }
            String root;
            final FileVisitor<Path> walker;
            switch (modificationType) {
                case RECURSIVE -> walker = new RecursiveVisitor(hasherAndWalker);
                case NON_RECURSIVE -> walker = new NonRecursiveVisitor(hasherAndWalker);
                case null, default -> throw new UnsupportedModificationError("null or default");
            }
            while ((root = in.readLine()) != null) {
                final Path rootPath;
                try {
                    rootPath = Path.of(root);
                } catch (InvalidPathException e) {
                    hasherAndWalker.writeZeroHash(root);
                    continue;
                }
                walkFileTree(rootPath, walker);
            }
        } catch (final IOException e) {
            System.err.println("Error with provided input/output file: " + e.getMessage());
        }
    }
    
    private static void printUsagePattern() {
        System.err.println("Usages");
        System.err.println("<input file with files and dirs> <output file>");
        System.err.println("<input file with files and dirs> <output file> <hashing method: sha-1 or jenkins>");
    }
}
