package info.kgeorgiy.ja.konovalov.implementor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * stores signature of a function in a collection of wrappers {@link Argument}
     */
    protected List<Argument> arguments;
    
    /**
     * method returnType class token, for constructors consider that ctor returns its new declared class
     */
    protected final Class<?> returnType;
    
    /**
     * Represents an Executable object, that is being represented
     */
    protected final Executable clazz;
    
    /**
     * fill all the fields of abstract method representation from a provided method
     * @param method method that is to be implemented
     */
    AbstractMethodRepresentation(final Method method) {
        if (Modifier.isPrivate(method.getReturnType().getModifiers())) {
            throw new UncheckedImplerException("private type for method result");
        }
        
        clazz = method;
        returnType = method.getReturnType();
        setSignature();
        
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
        clazz = ctor;
        
        setSignature();
    }
    
    /**
     * Generates throwing modifier
     * @return string starting with "throws" and then followed by all thrown exceptions, comma-separated
     */
    protected final String genThrows() {
        var exceptions = clazz.getExceptionTypes();
        
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
    private void setModifier(final int modifierInt) {
        if (Modifier.isPrivate(modifierInt)) {
            isEmpty = true;
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
     * Generates {@code arguments} {@code modifier}
     */
    protected final void setSignature() {
        arguments = Arrays.stream(clazz.getParameters()).map(Argument::new).collect(Collectors.toList());
        setModifier(clazz.getModifiers());
    }
    
    /**
     * Generates view of arguments created by provided mapper
     * @param mapper function that maps wrapper {@link Argument} to a string
     * @return function signature without return type and throws qualifiers
     */
    protected final String getArgumentsRepresentation(final Function<Argument, String> mapper) {
        return arguments.stream()
                        .map(mapper)
                        .collect(Collectors.joining(", "));
    }
    
    
    /**
     * Generates signature without brackets
     * @return signature without return type: so it looks like (type_i arg_i ...)
     */
    protected final String genArgsInSignature() {
        return getArgumentsRepresentation(e -> e.getArgumentType() + " " + e.getArgumentName());
    }
}
