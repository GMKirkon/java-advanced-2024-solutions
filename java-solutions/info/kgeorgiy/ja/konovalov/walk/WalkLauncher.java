package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.*;

public class WalkLauncher {
    
    private static final Map<Integer, String> possibleInputs = Map.of(
            0,
            "input",
            1,
            "output",
            2,
            "hashing type"
    );
    private final static EnumMap<HashingType, Hasher> possibleHashers = new EnumMap<>(Map.of(
            HashingType.JENKINS, new JenkinsHasher(),
            HashingType.SHA_1, new Sha1Hasher()
    ));
    
    public static void launch(WalkModifications modificationType, final String... args) {
        if (args == null || args.length < 2 || args.length > 3) {
            printUsagePattern();
            return;
        }
        
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                System.err.printf("Provided %s was null%n", possibleInputs.get(i));
                printUsagePattern();
                return;
            }
        }
        
        final HashingType hashingType;
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
        
        final Path inputFile = parseFileNameToPath(args[0], "input");
        final Path outputFile = parseFileNameToPath(args[1], "output");
        if (inputFile == null || outputFile == null) {
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
        
        try (final var in = newBufferedReader(inputFile, defaultCharset())) {
            try (final var out = newBufferedWriter(outputFile, defaultCharset())) {
                final Hasher hasher = possibleHashers.get(hashingType);
                final HashWriter writer = new HashWriter(out);
                final FileVisitor<Path> walker = modificationType.createWalker(writer, hasher);
                String root;
                try {
                    while ((root = in.readLine()) != null) {
                        try {
                            walkFileTree(Path.of(root), walker);
                        } catch (InvalidPathException e) {
                            try {
                                writer.writeHash(hasher.getErrorHash(), root);
                            } catch (IOException exc) {
                                System.err.println("Error with provided output file during writing: " + exc.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error with provided input file during reading: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Error with provided output file: " + e.getMessage());
            }
        } catch (final IOException e) {
            System.err.println("Error with provided input file: " + e.getMessage());
        }
    }
    
    private static void printUsagePattern() {
        System.err.println("Expected number of arguments from 2 to 3");
        System.err.println("Usages");
        System.err.println("<input file with files and dirs> <output file>");
        System.err.println("<input file with files and dirs> <output file> <hashing method: sha-1 or jenkins>");
    }
    
    static private Path parseFileNameToPath(String filename, String errorName) {
        try {
            return Path.of(filename);
        } catch (InvalidPathException e) {
            System.err.printf("Provided %s file was not correct: %s%n", errorName, e.getMessage());
            return null;
        }
    }
}
