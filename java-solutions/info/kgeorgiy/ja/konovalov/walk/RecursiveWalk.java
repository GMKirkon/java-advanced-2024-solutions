package info.kgeorgiy.ja.konovalov.walk;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Files.walkFileTree;

public class RecursiveWalk {
    
    public static void main(String... args) {
        
        if (args == null || args.length != 2) {
            printUsagePattern();
            return;
        }
        
        final Path inputFile;
        final Path outputFile;
        try {
            inputFile = Path.of(args[0]);
            outputFile = Path.of(args[1]);
        } catch (InvalidPathException e) {
            System.out.println("Provided input or output file was not correct: " + e.getMessage());
            return;
        }
        
        try (
                final var in = new BufferedReader(newBufferedReader(inputFile, UTF_8))
        ) {
            try (
                    final var out = new WalkWriter(newBufferedWriter(outputFile, UTF_8))
            ) {
                String root;
                var walker = new RecursiveWalker<>(out);
                while ((root = in.readLine()) != null) {
                    Path rootPath = Path.of(root);
                    try {
                        walkFileTree(rootPath, walker);
                    } catch (InvalidPathException e) {
                        out.writeHash(0, rootPath);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error with provided output file: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Error with provided input file: ");
        }
    }
    
    private static void printUsagePattern() {
        System.out.println("Usage: <input file with files and dirs> <output file>");
    }
}
