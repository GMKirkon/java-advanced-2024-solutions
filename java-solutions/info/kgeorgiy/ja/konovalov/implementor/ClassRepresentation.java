package info.kgeorgiy.ja.konovalov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class ClassRepresentation {
    
    ClassHeader header;
    Collection<MethodRepresentation> methodRepresentations;
    
    public ClassRepresentation(Class<?> token) throws ImplerException {
        header = new ClassHeader(token);
        // :NOTE: java.util.LinkedHashSet
        var allMethods = new java.util.LinkedHashSet<MethodRepresentation>();
        
        final Predicate<Executable> methodCheck = u ->
                !Modifier.isPrivate(u.getModifiers()) &&
                !Modifier.isStatic(u.getModifiers());
        
        final Predicate<Executable> ctorCheck = (
                // :NOTE: final constructor?
                u -> !Modifier.isFinal(u.getModifiers()) && methodCheck.test(u)
        );
        
        
        //add all ctors
        Arrays.stream(token.getDeclaredConstructors())
              .filter(ctorCheck)
              .map(e -> new MethodRepresentation(token, token.getSimpleName() + "Impl", e))
              .forEachOrdered(allMethods::add);
        
        
        if (allMethods.isEmpty() && !token.isInterface()) {
            throw new ImplerException("Could not implement utility class");
        }
        
        
        Consumer<Method[]> addAllToMethods =
                methods -> Arrays.stream(methods)
                                 .filter(methodCheck)
                                 .map(MethodRepresentation::new)
                                 .forEachOrdered(allMethods::add);
        
        addAllToMethods.accept(token.getMethods());
        
        //add all protected
        for (Class<?> currentToken = token; currentToken != null; currentToken = currentToken.getSuperclass()) {
            addAllToMethods.accept(currentToken.getDeclaredMethods());
        }
        
        
        methodRepresentations = allMethods.stream()
                                        // :NOTE: groupingBy string
                                          .collect(Collectors.groupingBy(MethodRepresentation::genSignature))
                                          .values()
                                          .stream()
                                          .map(representations -> representations.stream()
                                                  .min((MethodRepresentation a, MethodRepresentation b) -> a.returnType.isAssignableFrom(b.returnType) ? 1 : -1)
                                                 .orElse(null))
                                          .collect(toList());
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
        return methodRepresentations.stream().map(Objects::toString).collect(joining(""));
    }
}
