package info.kgeorgiy.ja.konovalov.iterative;

import java.util.AbstractList;
import java.util.List;

/**
 * Represents a list that represents a subset of another list by stepping through it in intervals.
 * The step size determines how big should be the step of iteration in original list.
 *
 * @param <T> the type of elements in the list
 */
public class SteppedList<T> extends AbstractList<T> {
    private final List<T> actualList;
    private final int step;
    
    /**
     * Constructs a new SteppedList with the given list and step size.
     *
     * @param list the list to be used
     * @param step the step size
     */
    public SteppedList(List<T> list, int step) {
        this.actualList = list;
        this.step = step;
    }
    
    @Override
    public T get(int index) {
        return actualList.get(index * step);
    }
    
    @Override
    public int size() {
        return (actualList.size() + step - 1) / step;
    }
}
