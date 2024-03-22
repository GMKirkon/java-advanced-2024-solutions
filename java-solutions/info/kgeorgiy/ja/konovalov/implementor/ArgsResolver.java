package info.kgeorgiy.ja.konovalov.implementor;

import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ArgsResolver {
    
    public static Collection<Argument> resolveArguments(Parameter... parameters) {
       return Arrays.stream(parameters).map(e -> new Argument(resolveSingle(e), e.getName()))
                    .collect(java.util.stream.Collectors.toList());
    }
    
    public static String resolveSingle(Parameter parameter) {
        if (Modifier.isPrivate(parameter.getType().getModifiers())) {
            throw new UncheckedImplerException("attempt to use private type");
        }
        return parameter.getType().getCanonicalName();
    }
}
