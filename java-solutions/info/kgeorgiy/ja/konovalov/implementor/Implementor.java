package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

public class Implementor implements Impler {
    
    public Implementor() {
    
    }

    // :NOTE: const
    static Set<Consumer<Class<?>>> prohibits = Set.of(
            Implementor::prohibitModifiersFromToken,
            Implementor::prohibitCornerClassTypes
    );
    
    private static void prohibitModifiersFromToken(final Class<?> token) {
        if (token.isInterface()) {
            prohibitInterfaceModifiersFromToken(token);
        } else {
            prohibitClassModifiersFromToken(token);
        }
    }
    
    private static void prohibitInterfaceModifiersFromToken(final Class<?> token) {
        prohibitInterfaceModifiers(token.getModifiers());
    }
    
    private static void prohibitClassModifiersFromToken(final Class<?> token) {
        prohibitClassModifiers(token.getModifiers());
    }
    
    private static void prohibitInterfaceModifiers(final int modifiers) {
        prohibitModifiersWithGivenString(modifiers, "implements");
    }
    
    private static void prohibitClassModifiers(final int modifiers) {
        prohibitModifiersWithGivenString(modifiers, "extends");
    }

    // :NOTE: too many levels of indirection
    private static void prohibitModifiersWithGivenString(final int modifiers, final String message) {
        prohibitPrivate(modifiers, message);
        prohibitFinal(modifiers, message);
    }
    
    private static void prohibitPrivate(final int modifiers, final String errorMessage) {
        if (Modifier.isPrivate(modifiers)) {
            throw new UncheckedImplerException(String.format("Cannot %s private interface", errorMessage));
        }
    }
    
    private static void prohibitFinal(final int modifers, final String errorMessage) {
        if (Modifier.isFinal(modifers)) {
            throw new UncheckedImplerException(String.format("Cannot %s final interface", errorMessage));
        }
    }
    
    public static void main(final String... args) throws ImplerException {
        if (args == null || args.length < 2 || args.length > 3) {
            printUsagePattern();
            return;
        }
        
        try {
            final Class<?> token = Class.forName(args[0]);
            final Path root = Path.of(args[1]);
            
            //cannot make implement static, :(
            final Implementor impl = new Implementor();
            impl.implement(token, root);
        } catch (final ClassNotFoundException e) {
            System.err.printf("Could not find class from provided name: %s%n", e.getMessage());
        } catch (final InvalidPathException e) {
            System.err.printf("Invalid path to outputroot : %s%n", e.getMessage());
        }
    }
    
    private static void printUsagePattern() {
        System.err.println("Expected number of arguments 2");
        System.err.println("Usages:%n");
        System.err.println("<className> <output root directory path>");
    }
    
    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        final Path destinationPath = root.resolve(
                Path.of(token.getPackageName().replace(".", File.separator))
                    .resolve(token.getSimpleName() + "Impl.java")
        );
        tryAccessParents(destinationPath);
        
        try (final var writer = Files.newBufferedWriter(destinationPath)) {
            prohibits.forEach(e -> e.accept(token));
            implementWithWriter(token, writer);
        } catch (final UncheckedImplerException e) {
            throw e.getImplerException();
        } catch (final IOException | SecurityException e) {
            throw new ImplerException(String.format("Error during writing: %s", e.getMessage()));
        }
    }

    // :NOTE: access?
    private static void tryAccessParents(final Path root) {
        final Path parent = root.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (final IOException e) {
                System.err.println("Warning, could not create parent directories to output path");
            }
        }
    }
    
    public void implementWithWriter(final Class<?> token, final Writer writer) throws ImplerException {
        final ClassRepresentation result = new ClassRepresentation(token);
        try {
            writer.write(result.toString());
        } catch (final IOException e) {
            throw new ImplerException(String.format("Error during printing the output %s", e.getMessage()));
        }
    }
    
    private static void prohibitCornerClassTypes(final Class<?> token) {
        if (token.isPrimitive()) {
            throw new UncheckedImplerException("Can't extend primitive type");
        }
        
        if (token == Enum.class) {
            throw new UncheckedImplerException("Can't extend Enum");
        }
        
        if (token == Record.class) {
            throw new UncheckedImplerException("Can't extend Record");
        }
    }
}
