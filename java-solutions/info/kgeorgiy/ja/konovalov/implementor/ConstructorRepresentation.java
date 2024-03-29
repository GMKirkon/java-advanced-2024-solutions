package info.kgeorgiy.ja.konovalov.implementor;

import java.lang.reflect.Constructor;

/**
 * Specialization of {@link AbstractMethodRepresentation} for constructors
 */
public class ConstructorRepresentation extends AbstractMethodRepresentation {
    /**
     * Constructor token used to store class
     */
    private final Constructor<?> ctor;
    
    /**
     * Creates ConstructorRepresentation
     * @param ctor constructor token
     */
    ConstructorRepresentation(Constructor<?> ctor) {
        super(ctor);
        this.ctor = ctor;
    }
    
    /**
     * @return java code for constructor
     */
    @Override
    public String toString() {
        if (isEmpty) {
            return String.format("%n");
        } else {
            return String.format(
                    "%s %s(%s) %s { %n %s %n return; %n  }%n",
                    modifier,
                    ctor.getDeclaringClass().getSimpleName() + "Impl",
                    genArgsInSignature(),
                    throwModifiers,
                    String.format("super(%s);", getArgumentsRepresentation(Argument::getArgumentName))
            );
        }
    }
}
