package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
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
import java.util.zip.ZipEntry;


/**
 * Class that creates default implementations of java classes/interfaces and its compressed .jar representations
 * from a provided {@link Class}
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
     * Method to use outside of Java to acces {@link #implement(Class, Path)} and {@link #implementJar(Class, Path)}
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
        } catch (ClassNotFoundException e) {
            System.err.printf("Could not find class from provided name: %s%n", e.getMessage());
        } catch (InvalidPathException e) {
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
     * Actual implementation for {@link Implementor#implement(Class, Path)}
     * <p>
     * Creates basic implementation for class that is provided by {@code token}
     * and writes it by specified path, to be more precisely, in a catalog that is needed,
     * inside the provided root directory
     * </p>
     *
     * @param token class/interface that is being implemented
     * @param root  path to root directory where to store the implementation
     * @throws ImplerException if provided class was impossible to implement,
     *                         or could not write the implementation to the destination
     * @see #implement(Class, Path)
     */
    public static void staticImplement(final Class<?> token, final Path root) throws ImplerException {
        final Path destinationPath = root.resolve(getClassPath(token, "java")).toAbsolutePath();
        tryAccessParents(destinationPath);
        
        try (var writer = Files.newBufferedWriter(destinationPath)) {
            prohibitModifiersFromToken(token);
            prohibitCornerClassTypes(token);
            
            implementWithWriter(token, writer);
        } catch (UncheckedImplerException e) {
            throw e.getImplerException();
        } catch (IOException | SecurityException e) {
            throw new IOImplerException(String.format("Error during writing: %s", e.getMessage()));
        }
    }
    
    
    /**
     * Generates actual java path for class
     *
     * @param token     type token for implemented/extended interface/class
     * @param extension .java or .class
     * @return Path that refers to needed implementation class
     */
    private static Path getClassPath(final Class<?> token, final String extension) {
        return Path.of(token.getPackageName().replace(".", File.separator))
                   .resolve(token.getSimpleName() + "Impl." + extension);
    }
    
    
    /**
     * Tries to create parent directory to provided root and prints warning if that is not possible
     *
     * @param root path to which parent directories has to be created
     */
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
    
    /**
     * Prohibits to implement classes/interfaces that are private or final
     *
     * @param token type token for class that is to be implemented by {@link #implement(Class, Path)}
     * @throws ImplerException if class/interface was final/private
     */
    private static void prohibitModifiersFromToken(final Class<?> token) throws ImplerException {
        if (token.isInterface()) {
            prohibitModifiersWithGivenString(token.getModifiers(), "implement", "interface");
        } else {
            prohibitModifiersWithGivenString(token.getModifiers(), "extend", "class");
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
     * Signature to {@link #implement(Class, Path)} with provided writer,
     * generate the code and outputs it with writer
     * wraps possible {@link IOException} to {@link ImplerException}
     *
     * @param token  type token for class that is to be implemented by {@link #implement(Class, Path)}
     * @param writer provided writer
     * @throws ImplerException in case could not output generated class or could not generate implementation
     */
    private static void implementWithWriter(final Class<?> token, final Writer writer) throws ImplerException {
        final ClassRepresentation result = new ClassRepresentation(token);
        try {
            writer.write(result.toString());
        } catch (IOException e) {
            throw new IOImplerException(String.format("Error during printing the output %s", e.getMessage()));
        }
    }
    
    /**
     * Prohibits(via throwing) private and final classes/interfaces
     *
     * @param modifiers       modifiers got from {@code token.getModifiers()}
     * @param inheritanceType implements or extends string to avoid copypaste
     * @param classType       interface or class string to avoid copypaste
     * @throws ImplerException if interface/class occurred to be final or private
     */
    private static void prohibitModifiersWithGivenString(
            final int modifiers,
            final String inheritanceType,
            final String classType
    ) throws ImplerException {
        if (Modifier.isPrivate(modifiers)) {
            throw new ImplerException(String.format("Cannot %s private %s", inheritanceType, classType));
        }
        if (Modifier.isFinal(modifiers)) {
            throw new ImplerException(String.format("Cannot %s final %s", inheritanceType, classType));
        }
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
     * @throws ImplerException if provided class was impossible to implement,
     *                         or could not create directories to store compiled files
     *                         or could not write the implementation to the destination
     * @see #implement(Class, Path)
     * @see #implementJar(Class, Path)
     */
    public static void staticImplementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        final Path buildDirectory;
        try {
            buildDirectory = Files.createTempDirectory(jarFile.getParent(), "jarImplementor");
        } catch (IOException e) {
            throw new IOImplerException("Unable to create temporary directory to store compiled files " + e.getMessage());
        }
        buildDirectory.toFile().deleteOnExit();
        
        staticImplement(token, buildDirectory);
        compile(token, buildDirectory, StandardCharsets.UTF_8);
        createJar(buildDirectory, getClassPath(token, "class"), jarFile);
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
     * @throws ImplerException if could not write implementation to provided jarFile
     */
    private static void createJar(final Path root, final Path classFile, final Path jarFile) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "Implementor");
        
        try (var jarStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            jarStream.putNextEntry(new ZipEntry(classFile.toString().replace(File.separator, "/")));
            Files.copy(root.resolve(classFile), jarStream);
        } catch (IOException e) {
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
}
