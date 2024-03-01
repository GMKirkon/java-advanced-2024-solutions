package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public class WalkLauncher {
    private static final java.util.List<String> POSSIBLE_INPUTS = java.util.List.of(
            "input",
            "output",
            "hashing type"
    );
    private static final EnumMap<HashingType, Hasher> POSSIBLE_HASHERS = new EnumMap<>(Map.of(
            HashingType.JENKINS, new JenkinsHasher(),
            HashingType.SHA_1, new Sha1Hasher()
    ));
    
    public static void launch(final WalkModifications modificationType, final String... args) {
        if (args == null || args.length < 2 || args.length > 3) {
            printUsagePattern();
            return;
        }
        
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                System.err.printf("Provided %s was null%n", POSSIBLE_INPUTS.get(i));
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
                default -> {
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
        
        // :NOTE: parse stderr
        final Path parent = outputFile.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (final IOException e) {
                System.err.println("could not create directories for outputFile path");
            }
        }
        
        try (final var in = Files.newBufferedReader(inputFile)) {
            try (final var out = Files.newBufferedWriter(outputFile)) {
                final Hasher hasher = POSSIBLE_HASHERS.get(hashingType);
                final HashWriter writer = new HashWriter(out);
                final FileVisitor<Path> walker = modificationType.createWalker(writer, hasher);
                String root;
                try {
                    while ((root = in.readLine()) != null) {
                        try {
                            Files.walkFileTree(Path.of(root), walker);
                        } catch (final InvalidPathException e) {
                            writer.writeHash(hasher.getErrorHash(), root);
                        }
                    }
                } catch (ImpossibleToOutputResult e) {
                    System.err.println("Error with provided output file during writing: " + e.getMessage());
                } catch (ImpossibleToProcessFileException e) {
                    System.err.println("Error with provided file during hashing: " + e.getMessage());
                } catch (final ImpossibleToOpenFile e) {
                    System.err.println("Error with provided input file during reading: " + e.getMessage());
                }
            } catch (final IOException e) {
                System.err.println("Error with provided output file during opening: " + e.getMessage());
            }
        } catch (final IOException e) {
            System.err.println("Error with provided input file during opening: " + e.getMessage());
        }
    }
    
    private static void printUsagePattern() {
        System.err.println("Expected number of arguments from 2 to 3");
        System.err.println("Usages");
        System.err.println("<input file with files and dirs> <output file>");
        System.err.println("<input file with files and dirs> <output file> <hashing method: sha-1 or jenkins>");
    }
    
    static private Path parseFileNameToPath(final String filename, final String errorName) {
        try {
            return Path.of(filename);
        } catch (final InvalidPathException e) {
            System.err.printf("Provided %s file was not correct: %s%n", errorName, e.getMessage());
            return null;
        }
    }
}
