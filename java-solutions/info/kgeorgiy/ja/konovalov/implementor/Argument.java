package info.kgeorgiy.ja.konovalov.implementor;

import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * Helper class to store single argument from method signature.
 * Stores {@link Parameter}, transforms it to correct Type and Name, so it can be used in code generation
 * @param parameter stored parameter used to know type and name of argument
 */
public record Argument(Parameter parameter) {
    
    /**
     * Creates Argument from given parameter
     * @param parameter stores parametor from method
     * @throws UncheckedImplerException if provided parameter's type is private
     */
    public Argument(final Parameter parameter) {
        this.parameter = parameter;
        if (Modifier.isPrivate(parameter.getType().getModifiers())) {
            throw new UncheckedImplerException("Cannot use private types");
        }
    }
    
    /**
     * Transforms to String argument.
     * @return Type argName, just like it is in signature
     */
    @Override
    public String toString() {
        return String.format("%s %s", getArgumentType(), getArgumentName());
    }
    
    
    /**
     * Returns full arguments type.
     * @return parameters argument Type canonical name
     */
    public String getArgumentType() {
        return parameter.getType().getCanonicalName();
    }
    
    /**
     * returns full argument name.
     * @return parameters argument name that is typically arg0, arg1... arg#N, where N is the position in signature
     */
    public String getArgumentName() {
        return parameter.getName();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Argument argument = (Argument) o;
        return Objects.equals(parameter.getType(), argument.parameter.getType());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(parameter.getType());
    }
}