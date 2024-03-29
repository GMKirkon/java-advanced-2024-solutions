package info.kgeorgiy.ja.konovalov.implementor;

// :NOTE: over engineering
/**
 * Helper class to store package, classname and class/interface that is extended/implemented.
 * That is combine called Header.
 * @see ClassRepresentation
 * @see MethodRepresentation
 */
public class ClassHeader {
    
    /**
     * Class's package, needed to be printed at the very first line of the .java file
     */
    public final String packageString;
    
    /**
     * Correct className for implementation. It is classNameImpl
     */
    public final String name;
    
    /**
     * Inheritance modifiers.
     * extends/implements and the realised class/interface
     */
    public final String extension;
    
    /**
     * Creates correct package, className and inheritance modifiers used in code generator
     * @param token classToken of implemented/extended class
     */
    ClassHeader(final Class<?> token) {
        packageString = String.format("package %s;", token.getPackageName());
        name = token.getSimpleName() + "Impl";
        extension = (token.isInterface() ? "implements " : "extends ") + token.getCanonicalName();
    }
    
    
    /**
     * @return String representation first two lines of implementation for class, that is called header
     */
    @Override
    public String toString() {
        return String.format("%s %n public class %s %s {%n", packageString, name, extension);
    }
}
