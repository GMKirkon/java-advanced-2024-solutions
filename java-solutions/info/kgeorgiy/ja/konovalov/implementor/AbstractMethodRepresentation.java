package info.kgeorgiy.ja.konovalov.implementor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

// :NOTE: access modifiers
/**
 *  Helper class to store constructions/methods
 */
public abstract class AbstractMethodRepresentation {
    /**
     * true if method should not be implemented, false otherwise
     */
    protected boolean isEmpty;
    
    /**
     * access modifier for the method
     */
    protected String modifier;
    /**
     * method returnType class token, for constructors consider that ctor returns its new declared class
     */
    protected Class<?> returnType;
    
    /**
     * stores signature of a function in a collection of wrappers {@link Argument}
     */
    protected Collection<Argument> arguments;
    
    /**
     * stores all thrown exceptions
     */
    protected String throwModifiers;
    
    /**
     * fill all the fields of abstract method representation from a provided method
     * @param method method that is to be implemented
     */
    AbstractMethodRepresentation(final Method method) {
        if (Modifier.isPrivate(method.getReturnType().getModifiers())) {
            throw new UncheckedImplerException("private type for method result");
        }
        
        returnType = method.getReturnType();
        setSignatureAndThrows(method);
        
        if (Modifier.isFinal(method.getModifiers()) || !Modifier.isAbstract(method.getModifiers())) {
            isEmpty = true;
        }
    }
    
    /**
     * fill all the fields of abstract method representation from a provided method
     * @param ctor constructor class that is to be implemented
     */
    AbstractMethodRepresentation(final Constructor<?> ctor) {
        returnType = ctor.getDeclaringClass();
        
        setSignatureAndThrows(ctor);
    }
    
    /**
     * Generates collection of arguments from array parameters
     * @param parameters parameters that are inside function signature
     * @return collection of wrapped in {@link Argument} functions arguments
     */
    protected static Collection<Argument> resolveArguments(final Parameter... parameters) {
        return Arrays.stream(parameters).map(Argument::new).collect(Collectors.toList());
    }
    
    
    /**
     * Generates throwing modifier
     * @param exceptions class tokens for thrown exceptions
     * @return string starting with "throws" and then followed by all thrown exceptions, comma-separated
     */
    protected static String genThrows(final Class<?>[] exceptions) {
        if (exceptions.length == 0) {
            return "";
        }
        return String.format(
                "throws %s",
                Arrays.stream(exceptions)
                      .map(Class::getCanonicalName)
                      .collect(Collectors.joining(", "))
        );
    }
    
    /**
     * Sets access modifier
     * Resolves access modifier to the method
     * @param modifierInt int from token.getModifiers() for current method
     */
    protected void setModifier(final int modifierInt) {
        if (Modifier.isPrivate(modifierInt)) {
            isEmpty = true;
            modifier = "";
        } else {
            isEmpty = false;
            if (Modifier.isPublic(modifierInt)) {
                modifier = "public";
            } else {
                if (Modifier.isProtected(modifierInt)) {
                    modifier = "protected";
                } else {
                    modifier = "";
                }
            }
        }
    }
    
    /**
     * Generates {@code arguments} {@code throwModifiers} {@code modifier}
     * @param method ctor or method, that are implemented
     */
    private void setSignatureAndThrows(final Executable method) {
        arguments = resolveArguments(method.getParameters());
        throwModifiers = genThrows(method.getExceptionTypes());
        setModifier(method.getModifiers());
    }
    
    /**
     * Generates view of arguments created by provided mapper
     * @param mapper function that maps wrapper {@link Argument} to a string
     * @return function signature without return type and throws qualifiers
     */
    protected String getArgumentsRepresentation(final Function<Argument, String> mapper) {
        return arguments.stream()
                        .map(mapper)
                        .collect(Collectors.joining(", "));
    }
    
    
    /**
     * Generates signature without brackets
     * @return signature without return type: so it looks like (type_i arg_i ...)
     */
    protected String genArgsInSignature() {
        return getArgumentsRepresentation(e -> e.getArgumentType() + " " + e.getArgumentName());
    }
}
