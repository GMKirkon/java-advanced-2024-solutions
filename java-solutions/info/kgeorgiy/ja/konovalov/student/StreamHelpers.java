package info.kgeorgiy.ja.konovalov.student;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamHelpers {
    public static <T, U extends Comparable<U>> List<T> sortCollectionByMethod(
            Collection<T> students,
            Function<T, U> method
    ) {
        return processToList(students, StreamAdapters.sortByFunction(method));
    }
    
    public static <T, U> List<U> processToList(Collection<T> students, Function<Stream<T>, Stream<U>> method) {
        return streamizeTheCollection(students, method, Collectors.toList());
    }
    
    public static <T, U, A, R> R streamizeTheCollection(
            Collection<T> students, Function<Stream<T>,
            Stream<U>> method,
            Collector<U, A, R> collector
    ) {
        return method.apply(students.stream()).collect(collector);
    }
    
    public static <T> List<T> sortCollectionByComparator(Collection<T> students, Comparator<T> comparator) {
        return processToList(students, StreamAdapters.sortByComparator(comparator));
    }
    
    public static <T> List<T> filterCollectionByPredicate(Collection<T> students, Predicate<T> method) {
        return processToList(students, StreamAdapters.filter(method));
    }
    
    public static <T, U> List<U> mapCollection(Collection<T> students, Function<T, U> method) {
        return processToList(students, StreamAdapters.map(method));
    }
    
    public static <T, U> U getMaxMapFromCollection(
            Collection<T> students, Comparator<T> comparator,
            Function<T, U> mapper,
            U defaultValue
    ) {
        return students.stream().max(comparator)
                       .map(mapper)
                       .orElse(defaultValue);
    }
}
