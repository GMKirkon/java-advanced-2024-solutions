package info.kgeorgiy.ja.konovalov.iterative;

import java.util.AbstractList;
import java.util.List;

public class SteppedList<T> extends AbstractList<T> {
    // :NOTE: не хватает модификаторов доступа
    List<T> actualList;
    int step;
    
    SteppedList(List<T> list, int step) {
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
