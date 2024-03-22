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
    String modifier;
    
    boolean isPrivate;
    
    String returnType;
    
    String name;
    
    String returnValue;
    Collection<Argument> arguments;
    
    String throwModifiers;
    String superCall;
    
    
    MethodRepresentation(Method method) {
        if (Modifier.isPrivate(method.getReturnType().getModifiers())) {
            throw new UncheckedImplerException("private type for method result");
        }
        returnType = method.getReturnType().getCanonicalName();
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
    
    MethodRepresentation(String typeName, Constructor<?> method) {
        returnType = typeName;
        returnValue = "";
        name = "";
        arguments = ArgsResolver.resolveArguments(method.getParameters());
        throwModifiers = genThrowNames(method.getExceptionTypes());
        superCall = genSuperCall();
        setModifier(method.getModifiers());
        if (Modifier.isFinal(method.getModifiers())) {
            isPrivate = true;
        }
    }
    
    void setModifier(int modifierInt) {
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
    
    String genReturnValue(Method method) {
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
    
    String genThrowNames(Class<?>[] exceptions) {
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
    
    
    String getArgumentsRepresentation(Function<Argument, String> mapper) {
        return String.format("%s", arguments.stream()
                                            .map(mapper)
                                            .collect(Collectors.joining(", ")));
    }
    String genSuperCall() {
        return String.format("super(%s);", getArgumentsRepresentation(Argument::getArgumentName));
    }
    
    String genArgsInSignature() {
        return  getArgumentsRepresentation(e -> e.argumentType + " " + e.argumentName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                arguments
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodRepresentation that = (MethodRepresentation) o;
        return Objects.equals(name, that.name) &&
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
                    returnType,
                    name,
                    genArgsInSignature(),
                    throwModifiers,
                    superCall,
                    returnValue
            );
        }
    }
}
