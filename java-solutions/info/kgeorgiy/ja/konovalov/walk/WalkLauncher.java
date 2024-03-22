package info.kgeorgiy.ja.konovalov.walk;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class WalkLauncher {
    private static final List<String> POSSIBLE_INPUTS = List.of(
            "input",
            "output",
            "hashing type"
    );
    private static final Map<Object, Hasher> POSSIBLE_HASHERS = Map.of(
            JenkinsHasher.class, new JenkinsHasher(),
            Sha1Hasher.class, new Sha1Hasher()
    );
    
    public static void launch(final BiFunction<HashWriter, Hasher, FileVisitor<Path>> walkerCreator, final String... args) {
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
        
        try {
            unsafeLaunch(walkerCreator, args);
        } catch (WalkingException e) {
            System.err.println(e.getMessage());
        }
    }
    
    private static void printUsagePattern() {
        System.err.println("Expected number of arguments from 2 to 3");
        System.err.println("Usages");
        System.err.println("<input file with files and dirs> <output file>");
        System.err.println("<input file with files and dirs> <output file> <hashing method: sha-1 or jenkins>");
    }
    
    public static void unsafeLaunch(final BiFunction<HashWriter, Hasher, FileVisitor<Path>> walkerCreator, final String... args) throws WalkingException {
        
        final Hasher hasher;
        if (args.length == 2) {
            hasher = POSSIBLE_HASHERS.get(JenkinsHasher.class);
        } else {
            switch (args[2]) {
                case "sha-1" -> hasher = POSSIBLE_HASHERS.get(Sha1Hasher.class);
                case "jenkins" -> hasher = POSSIBLE_HASHERS.get(JenkinsHasher.class);
                default -> {
                    printUsagePattern();
                    return;
                }
            }
        }
        
        final Path inputFile = parseFileNameToPath(args[0], "input");
        final Path outputFile = parseFileNameToPath(args[1], "output");
        
        final Path parent = outputFile.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (final IOException e) {
                System.err.println("Warning, could not create parent directories to output file");
            }
        }
        
        
        try (final var in = Files.newBufferedReader(inputFile)) {
            try (final var out = Files.newBufferedWriter(outputFile)) {
                final HashWriter writer = new HashWriter(out);
                final var walker = walkerCreator.apply(writer, hasher);
                String root;
                try {
                    while ((root = in.readLine()) != null) {
                        try {
                            Files.walkFileTree(Path.of(root), walker);
                        } catch (final InvalidPathException e) {
                            writer.writeHash(hasher.getErrorHash(), root);
                        }
                    }
                } catch (final IOException e) {
                    throw new WalkingException("Error processing input file");
                }
            } catch (final IOException e) {
                throw new ImpossibleToOpenFile("output file " + outputFile, e.getMessage());
            }
        } catch (final IOException e) {
            throw new ImpossibleToOpenFile("input file " + inputFile, e.getMessage());
        }
    }
    
    static private Path parseFileNameToPath(final String filename, final String errorName) {
        try {
            return Path.of(filename);
        } catch (final InvalidPathException e) {
            throw new WalkingException(String.format(
                    "Provided %s file was not correct: %s%n",
                    errorName,
                    e.getMessage()
            ));
        }
    }
}
