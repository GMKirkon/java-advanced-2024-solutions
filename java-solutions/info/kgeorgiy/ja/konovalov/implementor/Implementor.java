package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;


/**
 * Class that creates default implementations of java classes/interfaces and its compressed .jar representations
 * from a provided {@link Class}
 * <p>
 * {@link JarImpler}
 *
 * @see #implement(Class, Path)
 * @see #implementJar(Class, Path)
 */
public class Implementor implements JarImpler {
    
    /**
     * Default constructs the implementor
     * done only to avoid javadoc warning
     */
    public Implementor() {
    
    }
    
    /**
     * Method to use outside of Java to access {@link #implement(Class, Path)} and {@link #implementJar(Class, Path)}
     * use with two parameters for the first option
     * and with three parameters, first of which being "-jar" to access the second one.
     *
     * @param args arguments array used by out-of-code users
     * @throws ImplerException in same situations as accessed method
     * @see #implement(Class, Path)
     * @see #implementJar(Class, Path)
     */
    public static void main(final String... args) throws ImplerException {
        if (args == null || args.length < 2 || args.length > 3 ||
            (args.length == 3 && !Objects.equals(args[0], "-jar"))) {
            printUsagePattern();
            return;
        }
        
        try {
            final Class<?> token = Class.forName(args[args.length - 2]);
            final Path root = Path.of(args[args.length - 1]);
            if (args.length == 2) {
                staticImplement(token, root);
            } else {
                staticImplementJar(token, root);
            }
        } catch (final ClassNotFoundException e) {
            System.err.printf("Could not find class from provided name: %s%n", e.getMessage());
        } catch (final InvalidPathException e) {
            System.err.printf("Invalid path to output root : %s%n", e.getMessage());
        }
    }
    
    /**
     * signature method used to print how main should be used
     *
     * @see #main(String...)
     */
    private static void printUsagePattern() {
        System.err.println("Expected number of arguments 2 or 3");
        System.err.println("Usages:");
        System.err.println("<className> <output root directory path>");
        System.err.println("-jar <className> <file>.jar");
    }
    
    /**
     * Actual implementation for {@link Implementor#implement(Class, Path)}
     * <p>
     * Creates basic implementation for class that is provided by {@code token}
     * and writes it by specified path, to be more precisely, in a catalog that is needed,
     * inside the provided root directory
     * </p>
     *
     * @param token class/interface that is being implemented
     * @param root  path to root directory where to store the implementation
     * @throws ImplerException   if provided class was impossible to implement,
     * @throws IOImplerException if could not write the implementation to the destination
     * @see #implement(Class, Path)
     */
    public static void staticImplement(final Class<?> token, final Path root) throws ImplerException {
        prohibitModifiersFromToken(token);
        prohibitCornerClassTypes(token);
        
        final Path destinationPath = root.resolve(getClassPath(token, "java")).toAbsolutePath();
        tryCreateParents(destinationPath);
        
        try {
            String result = ClassGenerator.generateClassImplementation(token);
            try (final var writer = Files.newBufferedWriter(destinationPath)) {
                writer.write(transformToUnicode(result));
            } catch (final IOException | SecurityException e) {
                throw new IOImplerException(String.format("Error during writing: %s", e.getMessage()));
            }
        } catch (UncheckedImplerException e) {
            throw e.getImplerException();
        }
    }
    
    /**
     * Actual implementation for {@link Implementor#implementJar(Class, Path)}
     * <p>
     * Creates basic implementation for class that is provided by {@code token}
     * stores it to temporally created directory, then created jar archive that is stored to the provided
     * inside the provided root directory
     * </p>
     *
     * @param token   class/interface that is being implemented
     * @param jarFile target .jar file.
     * @throws ImplerException   if provided class was impossible to implement,
     * @throws IOImplerException if could not create directories to store compiled files
     *                           or could not write the implementation to the destination
     * @see #implement(Class, Path)
     * @see #implementJar(Class, Path)
     */
    public static void staticImplementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        final Path buildDirectory;
        try {
            buildDirectory = Files.createTempDirectory(jarFile.getParent(), "jarImplementor");
        } catch (final IOException | IllegalArgumentException | SecurityException | UnsupportedOperationException e) {
            throw new IOImplerException("Unable to create temporary directory to store compiled files " + e.getMessage());
        }
        
        staticImplement(token, buildDirectory);
        compile(token, buildDirectory, StandardCharsets.UTF_8);
        createJar(buildDirectory, getClassPath(token, "class"), jarFile);
    }
    
    /**
     * Prohibits to implement classes/interfaces that are private or final
     *
     * @param token type token for class that is to be implemented by {@link #implement(Class, Path)}
     * @throws ImplerException if class/interface was final/private
     */
    private static void prohibitModifiersFromToken(final Class<?> token) throws ImplerException {
        int modifiers = token.getModifiers();
        if (Modifier.isPrivate(modifiers)) {
            throw new ImplerException(String.format(
                    "Cannot generate implementation for private class %s",
                    token.getCanonicalName()
            ));
        }
        if (Modifier.isFinal(modifiers)) {
            throw new ImplerException(String.format(
                    "Cannot generate implementation for final class %s",
                    token.getCanonicalName()
            ));
        }
    }
    
    /**
     * Prohibits to implement corner-cases classes/interfaces such as primitives, enum, record
     *
     * @param token type token for class that is to be implemented by {@link #implement(Class, Path)}
     * @throws ImplerException if provided class was primitive, or enum or a record
     */
    private static void prohibitCornerClassTypes(final Class<?> token) throws ImplerException {
        if (token.isPrimitive()) {
            throw new ImplerException("Can't extend primitive type");
        }
        
        if (token == Enum.class) {
            throw new ImplerException("Can't extend Enum");
        }
        
        if (token == Record.class) {
            throw new ImplerException("Can't extend Record");
        }
    }
    
    /**
     * Generates actual java path for class
     *
     * @param token     type token for implemented/extended interface/class
     * @param extension java or class
     * @return Path that refers to needed implementation class
     */
    private static Path getClassPath(final Class<?> token, final String extension) {
        return Path.of(token.getPackageName().replace(".", File.separator))
                   .resolve(token.getSimpleName() + "Impl." + extension);
    }
    
    /**
     * A utility method used to generate the directory structure (parent directories) for a given path.
     *
     * @param root target path for which directories need to be created
     */
    private static void tryCreateParents(final Path root) {
        final Path parent = root.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (final IOException e) {
                System.err.println("Warning, could not create parent directories to output path");
            }
        }
    }
    
    /**
     * Transforms the given implementation code to Unicode escape sequences for characters outside the ASCII range.
     *
     * @param implementationCode the implementation code to transform
     * @return the transformed implementation code
     */
    private static String transformToUnicode(String implementationCode) {
        return implementationCode.chars()
                                 .mapToObj(c -> c >= 128 ? String.format("\\u%04X", c) : String.valueOf((char) c))
                                 .collect(Collectors.joining());
    }
    
    /**
     * Compiles class (that is given by its {@code Class} token, that is stored at {@code root} path,
     * with provided charset
     *
     * @param token   implemented class type token
     * @param root    path to implementations
     * @param charset charset to store compiled .class file with
     * @throws ImplerException if could not find java compiler or some error occurred during compilation
     */
    private static void compile(final Class<?> token, final Path root, final Charset charset) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }
        
        final String classpath = getClassPath(token);
        final String[] args = {
                root.resolve(getClassPath(token, "java")).toString(),
                "-cp",
                classpath,
                "-encoding",
                charset.name()
        };
        
        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Could not compile, some error occur");
        }
    }
    
    /**
     * Creates jar archive to store implementation of one provided class
     *
     * @param root      path to directory where to search implemented .class file
     * @param classFile path to class implementation
     * @param jarFile   target .jar file.
     * @throws IOImplerException if cannot not write implementation to provided jarFile
     */
    private static void createJar(final Path root, final Path classFile, final Path jarFile) throws IOImplerException {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "Implementor");
        
        try (final var jarStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            jarStream.putNextEntry(new ZipEntry(classFile.toString().replace(File.separator, "/")));
            Files.copy(root.resolve(classFile), jarStream);
        } catch (final IOException e) {
            throw new IOImplerException(String.format(
                    "Could not output to jar archive with problem : %s",
                    e.getMessage()
            ));
        }
    }
    
    /**
     * Magically generating classpath from class token
     *
     * @param token class which classpath is to be got
     * @return classpath for provided class
     */
    private static String getClassPath(final Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }
    
    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * <p>
     * For more implementation details see documentation to {@link #staticImplement(Class, Path)}
     */
    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        staticImplement(token, root);
    }
    
    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * <p>
     * For more implementation details see documentation to {@link #staticImplementJar(Class, Path)}
     */
    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        staticImplementJar(token, jarFile);
    }
}
