package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

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
    
    static Set<Consumer<Class<?>>> prohibits = Set.of(
            Implementor::prohibitModifiersFromToken,
            Implementor::prohibitCornerClassTypes
    );
    
    private static void prohibitModifiersFromToken(Class<?> token) {
        if (token.isInterface()) {
            prohibitInterfaceModifiersFromToken(token);
        } else {
            prohibitClassModifiersFromToken(token);
        }
    }
    
    private static void prohibitInterfaceModifiersFromToken(Class<?> token) {
        prohibitInterfaceModifiers(token.getModifiers());
    }
    
    private static void prohibitClassModifiersFromToken(Class<?> token) {
        prohibitClassModifiers(token.getModifiers());
    }
    
    private static void prohibitInterfaceModifiers(int modifiers) {
        prohibitModifiersWithGivenString(modifiers, "implements");
    }
    
    private static void prohibitClassModifiers(int modifiers) {
        prohibitModifiersWithGivenString(modifiers, "extends");
    }
    
    private static void prohibitModifiersWithGivenString(int modifiers, String message) {
        prohibitPrivate(modifiers, message);
        prohibitFinal(modifiers, message);
    }
    
    private static void prohibitPrivate(int modifers, String errorMessage) {
        if (Modifier.isPrivate(modifers)) {
            throw new UncheckedImplerException(String.format("Cannot %s private interface", errorMessage));
        }
    }
    
    private static void prohibitFinal(int modifers, String errorMessage) {
        if (Modifier.isFinal(modifers)) {
            throw new UncheckedImplerException(String.format("Cannot %s final interface", errorMessage));
        }
    }
    
    public static void main(String... args) throws ImplerException {
        if (args == null || args.length < 2 || args.length > 3) {
            printUsagePattern();
            return;
        }
        
        try {
            Class<?> token = Class.forName(args[0]);
            Path root = Path.of(args[1]);
            
            //cannot make implement static, :(
            Implementor impl = new Implementor();
            impl.implement(token, root);
        } catch (ClassNotFoundException e) {
            System.err.printf("Could not find class from provided name: %s%n", e.getMessage());
        } catch (InvalidPathException e) {
            System.err.printf("Invalid path to outputroot : %s%n", e.getMessage());
        }
    }
    
    private static void printUsagePattern() {
        System.err.println("Expected number of arguments 2");
        System.err.println("Usages:%n");
        System.err.println("<className> <output root directory path>");
    }
    
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        final Path destinationPath = root.resolve(
                Path.of(token.getPackageName().replace('.', java.io.File.separatorChar))
                    .resolve(token.getSimpleName() + "Impl.java"));
        tryAccessParents(destinationPath);
        
        try (var writer = Files.newBufferedWriter(destinationPath)) {
            prohibits.forEach(e -> e.accept(token));
            implementWithWriter(token, writer);
        } catch (UncheckedImplerException e) {
            throw e.getImplerException();
        } catch (IOException | SecurityException e) {
            throw new ImplerException(String.format("Error during writing: %s", e.getMessage()));
        }
    }
    
    private void tryAccessParents(Path root) {
        final Path parent = root.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (final IOException e) {
                System.err.println("Warning, could not create parent directories to output path");
            }
        }
    }
    
    public void implementWithWriter(Class<?> token, Writer writer) throws ImplerException {
        ClassRepresentation result = new ClassRepresentation(token);
        try {
            writer.write(result.toString());
        } catch (IOException e) {
            throw new ImplerException(String.format("Error during printing the output %s", e.getMessage()));
        }
    }
    
    private static void prohibitCornerClassTypes(Class<?> token) {
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
