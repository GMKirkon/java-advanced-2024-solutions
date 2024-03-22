package info.kgeorgiy.ja.konovalov.student;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class StreamAdapters {
    
    public static <T> Function<Stream<T>, Stream<T>> filter(Predicate<T> predicate) {
        return (stream -> stream.filter(predicate));
    }
    
    public static <T, U> Function<Stream<T>, Stream<U>> map(Function<T, U> mapper) {
        return (stream -> stream.map(mapper));
    }
    
    public static <T> Function<Stream<T>, Stream<T>> max(Comparator<T> comparator) {
        return (stream -> stream.max(comparator).stream());
    }
    
    public static <T, U extends Comparable<? super U>> Function<Stream<T>, Stream<T>> sortByFunction(Function<T, U> method) {
        return sortByComparator(Comparator.comparing(method));
    }
    
    public static <T> Function<Stream<T>, Stream<T>> sortByComparator(Comparator<T> comparator) {
        return (a -> a.sorted(comparator));
    }
    
}
