package info.kgeorgiy.ja.konovalov.implementor;

public class Argument {
    String argumentType;
    String argumentName;
    
    public Argument(String argumentType, String argumentName) {
        this.argumentType = argumentType;
        this.argumentName = argumentName;
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(argumentType);
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
        return java.util.Objects.equals(argumentType, argument.argumentType);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", argumentType, argumentName);
    }
    
    public String getArgumentType() {
        return argumentType;
    }
    
    public String getArgumentName() {
        return argumentName;
    }
}