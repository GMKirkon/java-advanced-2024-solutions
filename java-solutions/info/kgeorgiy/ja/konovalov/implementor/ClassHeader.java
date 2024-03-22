package info.kgeorgiy.ja.konovalov.implementor;

public class ClassHeader {
    
    public final String packageString;
    
    public final String name;
    
    public final String type;
    
    public final String extension;
    
    ClassHeader(Class<?> token) {
        packageString = String.format("package %s;", token.getPackageName());
        name = token.getSimpleName() + "Impl";
        type = "class";
        extension = (token.isInterface() ? "implements " : "extends ") + token.getCanonicalName();
    }
    
    @Override
    public String toString() {
        return String.format("%s %n public %s %s %s {%n", packageString, type, name, extension);
    }
}
