package info.kgeorgiy.ja.konovalov.implementor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility-Class generating implementation for provided typetoken.
 */
public final class ClassGenerator {
    
    /**
     * Predicate that determines whether it is possible to implement {@link Method}
     * Checks that method is not private and is not static
     */
    private static final Predicate<Method> METHOD_CHECK = u ->
            !Modifier.isPrivate(u.getModifiers()) &&
            !Modifier.isStatic(u.getModifiers());
    /**
     * Predicate that determines whether it is possible to implement {@link Constructor}
     * Checks that method is not private
     */
    private static final Predicate<Constructor<?>> CTORS_CHECK = u ->
            !Modifier.isPrivate(u.getModifiers());
    
    /**
     * Private ctor for utility class ClassGenerator.
     */
    private ClassGenerator() {
    
    }
    
    /**
     * Generates a string representing the full implementation for the given class.
     *
     * <p>The implementation includes:
     * <ul>
     * <li>Package statement</li>
     * <li>Class declaration (including implementing or extending the original class)</li>
     * <li>Implementations for all non-private constructors using {@link ConstructorRepresentation}</li>
     * <li>Implementations for all non-private, non-static inherited methods using {@link MethodRepresentation}</li>
     * </ul>
     *
     * <p>The constructors and method implementations are generated in the order they appear in the original class.
     *
     * @param token the class token representing the class to be implemented
     * @return a string representing the full implementation of the given class
     */
    public static String generateClassImplementation(Class<?> token) {
        return String.format(
                "package %s; %n public class %s %s {%n %s }",
                token.getPackageName(),
                token.getSimpleName() + "Impl",
                (token.isInterface() ? "implements " : "extends ") + token.getCanonicalName(),
                generateCtorsAndMethods(token)
        );
    }
    
    /**
     * Creates ClassGenerator from provided class token.
     * Generated header, all non-private constructors, and all non-private, non-static inherited methods,
     * that return default values. Uses {@link MethodRepresentation} to generate all methods and constructors.
     *
     * @param token classToken that is used to represent the class/interface for which
     *              the implementation has to be implemented
     * @return String with implementation of all (non-private)constuctors and (non-private, non-static)methods
     * of provided typetoken, that has to be generated in order to compile class
     * @throws UncheckedImplerException if some functions that has to be implemented use some private Types, that are not visible
     */
    private static String generateCtorsAndMethods(final Class<?> token) throws UncheckedImplerException {
        List<String> ctors;
        
        ctors = Arrays.stream(token.getDeclaredConstructors())
                      .filter(CTORS_CHECK)
                      .map(ConstructorRepresentation::new)
                      .map(ConstructorRepresentation::toString)
                      .toList();
        
        if (ctors.isEmpty() && !token.isInterface()) {
            throw new UncheckedImplerException("Could not implement utility class");
        }
        
        Stream<Method> methodStream = Arrays.stream(token.getMethods());
        for (Class<?> currentToken = token; currentToken != null; currentToken = currentToken.getSuperclass()) {
            methodStream = Stream.concat(methodStream, Arrays.stream(currentToken.getDeclaredMethods()));
        }
        
        var actualMethodsStream =
                methodStream.filter(METHOD_CHECK).map(MethodRepresentation::new)
                            .collect(Collectors.groupingBy(MethodRepresentation::getName))
                            .values().stream()
                            .flatMap(
                                    representations ->
                                            representations.stream().collect(
                                                    Collectors.toMap(
                                                            e -> e.arguments,
                                                            e -> e,
                                                            (method1, method2) -> method1.compareTo(method2) <= 0 ? method1 : method2
                                                    )).values().stream()
                            )
                            .map(MethodRepresentation::toString);
        
        return Stream.concat(ctors.stream(), actualMethodsStream).collect(Collectors.joining(""));
    }
}
