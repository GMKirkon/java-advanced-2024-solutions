package info.kgeorgiy.ja.konovalov.implementor;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Specialization of {@link AbstractMethodRepresentation} for methods
 */
public class MethodRepresentation extends AbstractMethodRepresentation implements Comparable<MethodRepresentation> {
    
    
    /**
     * Creates Method representation
     *
     * @param method method that is to be implemented
     */
    MethodRepresentation(Method method) {
        super(method);
    }
    
    /**
     * Generated default value that is used for provided method's return type
     *
     * @param method method for which the return value is being generated
     * @return default value that can be returned from the method
     */
    private static String genDefaultReturnValue(final Method method) {
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
    
    /**
     * getter to be provided to {@link ClassGenerator} for groupingBy
     *
     * @return methods name
     */
    String getName() {
        return clazz.getName();
    }
    
    /**
     * Compares Methods by return type only. Should be used on methods with same signature only!
     * @param o the object to be compared.
     * @return 0 if returnTypes are the same, 1 if returnType is lower than o.returnType, -1 otherwise
     */
    @Override
    public int compareTo(MethodRepresentation o) {
        // :NOTE: not consistent with equals
        /* Could be used for debug, that is slow, so it is commented
        * #IFDEF(DEBUG)
        if (!arguments.equals(o.arguments)) {
            throw new AssertionError("cannot compare two methods with different signature");
        }
        */
        
        if (returnType == o.returnType) {
            return 0;
        }
        return returnType.isAssignableFrom(o.returnType) ? 1 : -1;
    }
    
    /**
     * @return hashcode produced by signature: return type name and arguments
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                returnType,
                clazz.getName(),
                arguments
        );
    }
    
    /**
     * @param o other object
     * @return true if object has the same signature in other words: returnType, name and arguments.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        MethodRepresentation that = (MethodRepresentation) o;
        
        return Objects.equals(arguments, that.arguments) &&
               Objects.equals(getName(), that.getName());
    }
    
    /**
     * @return java code for method
     */
    @Override
    public String toString() {
        if (isEmpty) {
            return "";
        } else {
            return String.format(
                    "%s %s %s(%s) %s { %n return %s; %n  }%n",
                    modifier,
                    returnType.getCanonicalName(),
                    getName(),
                    genArgsInSignature(),
                    genThrows(),
                    genDefaultReturnValue((Method) clazz)
            );
        }
    }
}
