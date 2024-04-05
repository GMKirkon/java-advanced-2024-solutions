package info.kgeorgiy.ja.konovalov.implementor;

import java.lang.reflect.Constructor;

/**
 * Specialization of {@link AbstractMethodRepresentation} for constructors
 */
public class ConstructorRepresentation extends AbstractMethodRepresentation {
    
    /**
     * Creates ConstructorRepresentation
     * @param ctor constructor token
     */
    ConstructorRepresentation(Constructor<?> ctor) {
        super(ctor);
    }
    
    /**
     * @return java code for constructor
     */
    @Override
    public String toString() {
        if (isEmpty) {
            return "";
        } else {
            return String.format(
                    "%s %s(%s) %s { %n %s %n return; %n  }%n",
                    modifier,
                    clazz.getDeclaringClass().getSimpleName() + "Impl",
                    genArgsInSignature(),
                    genThrows(),
                    String.format("super(%s);", getArgumentsRepresentation(Argument::getArgumentName))
            );
        }
    }
    
    
}

