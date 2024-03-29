package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Helper class used to generate and store class implementation.
 *
 * @see Implementor#implement(Class, Path)
 * @see ClassHeader
 * @see MethodRepresentation
 */
public class ClassRepresentation {
    
    /**
     * Representation of first two lines of class implementation,
     * its package and name with implemented/extended interface/class
     */
    final private ClassHeader header;
    /**
     * Collection of signature wrapper classes {@link AbstractMethodRepresentation}
     * that is used to store all methods and ctors implementation
     */
    final private Collection<AbstractMethodRepresentation> methodRepresentations;
    
    
    /**
     * Predicated used to determine whether it is possible to implement method represented by
     * {@link Method} or {@link Constructor}, that is type-erased to {@link Executable}
     * Checks that method/constructor is not private and is not static
     */
    // :NOTE: CONST
    private static final Predicate<Executable> methodCheck = u ->
            !Modifier.isPrivate(u.getModifiers()) &&
            !Modifier.isStatic(u.getModifiers());
    /**
     * Function to produce wrapper classes {@link MethodRepresentation} from raw array of {@link Method}
     */
    private static final Function<Method[], Stream<MethodRepresentation>> transformAllMethods =
            methods -> filterMethods(methods).map(MethodRepresentation::new);
    /**
     * Function to produce wrapper classes {@link ConstructorRepresentation} from raw array of {@link Constructor}
     */
    private static final Function<Constructor<?>[], Stream<ConstructorRepresentation>> transformAllCtors =
            methods -> filterMethods(methods).map(ConstructorRepresentation::new);
    
    /**
     * Generic method to generate function that
     * adds array of {@link  Method} or {@link Constructor}
     * to collection of signature classes {@link AbstractMethodRepresentation}
     *
     * @param transformer function used to create {@link Stream<AbstractMethodRepresentation>} from provided array
     * @param addder      function(almost always method reference) to add {@link AbstractMethodRepresentation} to some Collection
     * @param <T>         {@link  Method} or {@link Constructor}
     * @param <U>         {@link MethodRepresentation} or {@link ConstructorRepresentation}
     * @return function that can be used to add raw array of Methods/Ctors to collection
     */
    private static <T, U> Consumer<T[]> addAllMethodsToCollection(
            final Function<T[], Stream<U>> transformer,
            final Consumer<U> addder
    ) {
        return e -> transformer.apply(e).forEachOrdered(addder);
    }
    
    /**
     * Function used to produce Stream of methods/ctors that have to be implemented
     *
     * @param methods array of functions to implement
     * @param <T>     {@link  Method} or {@link Constructor}
     * @return Stream consisting of only implementable functions
     */
    private static <T extends Executable> Stream<T> filterMethods(T[] methods) {
        return Arrays.stream(methods).filter(methodCheck);
    }
    
    
    /**
     * Creates ClassRepresentation from provided class token.
     * Generated header, all non-private constructors, and all non-private, non-static inherited methods,
     * that return default values. Uses {@link MethodRepresentation} to generate all methods and constructors.
     * Uses {@link ClassHeader} to generate header.
     *
     * @param token classToken that is used to represent the class/interface for which
     *              the implementation has to be implemented
     * @throws ImplerException          if all constructors are private
     * @throws UncheckedImplerException if some functions that has to be implemented use some private Types, that are not visible
     */
    public ClassRepresentation(final Class<?> token) throws ImplerException {
        header = new ClassHeader(token);
        var methods = new HashSet<MethodRepresentation>();
        var ctors = new HashSet<ConstructorRepresentation>();

        // :NOTE: simplify
        addAllMethodsToCollection(transformAllCtors, ctors::add).accept(token.getDeclaredConstructors());
        
        if (ctors.isEmpty() && !token.isInterface()) {
            throw new ImplerException("Could not implement utility class");
        }

        // :NOTE: stream
        addAllMethodsToCollection(transformAllMethods, methods::add).accept(token.getMethods());
        
        for (Class<?> currentToken = token; currentToken != null; currentToken = currentToken.getSuperclass()) {
            addAllMethodsToCollection(transformAllMethods, methods::add).accept(currentToken.getDeclaredMethods());
        }
        
        methodRepresentations =
                methods.stream()
                       .collect(Collectors.groupingBy(MethodRepresentation::getName))
                       .values().stream()
                        // :NOTE: ??
                       .flatMap(
                            representations ->
                                representations.stream()
                                .collect(Collectors.groupingBy((MethodRepresentation e) -> e.arguments.toString()))
                                .values().stream()
                       )
                       .map(e -> e.stream().min(MethodRepresentation::compareTo).get()).collect(toList());
        
        methodRepresentations.addAll(ctors);
    }
    

    
    /**
     * @return full class implementation
     */
    @Override
    public String toString() {
        return String.format(
                "%s %s }",
                header.toString(),
                methodRepresentations.stream().map(Objects::toString).collect(joining(""))
        );
    }
}
