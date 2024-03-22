package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClassRepresentation {
    
    ClassHeader header;
    Collection<MethodRepresentation> methodRepresentations;
    
    public ClassRepresentation(Class<?> token) throws ImplerException {
        header = new ClassHeader(token);
        var allMethods = new java.util.LinkedHashSet<MethodRepresentation>();
        
        Predicate<Executable> methodCheck = u ->
                !Modifier.isPrivate(u.getModifiers()) &&
                !Modifier.isStatic(u.getModifiers());
        
        Predicate<Executable> ctorCheck = (
                u -> !Modifier.isFinal(u.getModifiers()) && methodCheck.test(u)
        );
        
        //add all ctors
        Arrays.stream(token.getDeclaredConstructors())
              .filter(ctorCheck)
              .map(e -> new MethodRepresentation(header.name, e))
              .forEachOrdered(allMethods::add);
        
        
        if (allMethods.isEmpty() && !token.isInterface()) {
            throw new ImplerException("Could not implement utility class");
        }
        
        
        Consumer<Method[]> addAllToMethods =
                methods -> Arrays.stream(methods)
                                 .filter(methodCheck)
                                 .map(MethodRepresentation::new)
                                 .forEachOrdered(allMethods::add);
        
        //add all public methods
        addAllToMethods.accept(token.getMethods());
        
        //add all protected
        for (Class<?> currentToken = token; currentToken != null; currentToken = currentToken.getSuperclass()) {
            addAllToMethods.accept(currentToken.getDeclaredMethods());
        }
        
        methodRepresentations = allMethods.stream().distinct().toList();
    }
    
    @Override
    public String toString() {
        return String.format(
                "%s %s }",
                header.toString(),
                genAllMethods()
        );
    }
    
    private String genAllMethods() {
        return methodRepresentations.stream().map(Objects::toString).collect(Collectors.joining(""));
    }
}
