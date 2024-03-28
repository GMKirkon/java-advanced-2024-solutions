package info.kgeorgiy.ja.konovalov.implementor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MethodRepresentation {
    // :NOTE: access modifiers
    String modifier;
    
    boolean isPrivate;
    
    Class<?> returnType;
    String returnTypeName;
    
    String name;
    
    String returnValue;
    Collection<Argument> arguments;
    
    String throwModifiers;
    String superCall;
    
    
    MethodRepresentation(final Method method) {
        if (Modifier.isPrivate(method.getReturnType().getModifiers())) {
            throw new UncheckedImplerException("private type for method result");
        }
        returnType = method.getReturnType();
        returnTypeName = returnType.getCanonicalName();
        returnValue = genReturnValue(method);
        name = method.getName();
        arguments = ArgsResolver.resolveArguments(method.getParameters());
        throwModifiers = genThrowNames(method.getExceptionTypes());
        superCall = "";
        setModifier(method.getModifiers());
        if (Modifier.isFinal(method.getModifiers()) || !Modifier.isAbstract(method.getModifiers())) {
            isPrivate = true;
        }
    }
    
    MethodRepresentation(final Class<?> returnType, final String returnTypeName, final Constructor<?> constructor) {
        this.returnType = returnType;
        this.returnTypeName = returnTypeName;
        returnValue = "";
        name = "";
        arguments = ArgsResolver.resolveArguments(constructor.getParameters());
        throwModifiers = genThrowNames(constructor.getExceptionTypes());
        superCall = String.format("super(%s);", getArgumentsRepresentation(Argument::getArgumentName));
        setModifier(constructor.getModifiers());
        if (Modifier.isFinal(constructor.getModifiers())) {
            isPrivate = true;
        }
    }
    
    void setModifier(final int modifierInt) {
        if (Modifier.isPrivate(modifierInt)) {
            isPrivate = true;
            modifier = "";
        } else {
            isPrivate = false;
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
    
    String genReturnValue(final Method method) {
        if (!method.getReturnType().isPrimitive()) {
            return "null";
        } else if (method.getReturnType() == void.class) {
            return "";
        } else if (method.getReturnType() != boolean.class) {
            return "0";
        } else {
            return "false";
        }
    }
    
    String genThrowNames(final Class<?>[] exceptions) {
        if (exceptions.length == 0) {
            return "";
        }
        return String.format(
                "throws %s",
                Arrays.stream(exceptions)
                      .map(Class::getName)
                      .collect(Collectors.joining(", "))
        );
    }
    
    
    String getArgumentsRepresentation(final Function<Argument, String> mapper) {
        // :NOTE: String.format("%s"
        return String.format("%s", arguments.stream()
                                            .map(mapper)
                                            .collect(Collectors.joining(", ")));
    }
    String genSuperCall() {
        return String.format("super(%s);", getArgumentsRepresentation(Argument::getArgumentName));
    }
    
    String genArgsInSignature() {
        return getArgumentsRepresentation(e -> e.argumentType + " " + e.argumentName);
    }
    
    String genSignature() {
        return name + genArgsInSignature();
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(
                returnType,
                name,
                arguments
        );
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MethodRepresentation that = (MethodRepresentation) o;
        return Objects.equals(returnType, that.returnType) &&
               Objects.equals(name, that.name) &&
               Objects.equals(arguments, that.arguments);
    }
    
    @Override
    public String toString() {
        if (isPrivate) {
            return String.format("%n");
        } else {
            return String.format(
                    "%s %s %s(%s) %s { %n %s %n  return %s; %n  }%n",
                    modifier,
                    returnTypeName,
                    name,
                    genArgsInSignature(),
                    throwModifiers,
                    superCall,
                    returnValue
            );
        }
    }
}
