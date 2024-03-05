package info.kgeorgiy.ja.konovalov.arrayset;

import java.util.*;

import static java.lang.Integer.min;

public class ArraySet<E> extends AbstractList<E> implements NavigableSet<E>, List<E> {
    
    private final Comparator<? super E> cmp;
    private final List<E> list;
    
    public ArraySet() {
        cmp = null;
        list = Collections.emptyList();
    }
    
    public ArraySet(Collection<? extends E> collection) {
        cmp = null;
        list = Collections.unmodifiableList(makeUnique(collection));
    }
    
    private List<E> makeUnique(Collection<? extends E> collection) {
        ArrayList<E> tmp = new java.util.ArrayList<>(collection);
        tmp.sort(cmp);
        int left = 1;
        for (int right = 1; right < tmp.size(); right++) {
            if (actualCompare(tmp.get(left - 1), tmp.get(right)) != 0) {
                Collections.swap(tmp, left, right);
                left++;
            }
        }
        return tmp.subList(0, min(left, tmp.size()));
    }
    
    @SuppressWarnings("unchecked")
    private int actualCompare(E a, E b) {
        if (cmp == null) {
            return ((Comparable<E>) a).compareTo(b);
        } else {
            return cmp.compare(a, b);
        }
    }
    
    public ArraySet(Comparator<? super E> comparator) {
        cmp = comparator;
        list = Collections.emptyList();
    }
    
    //ah, old good semantics by naming... One day I hope there will be templates, not generics.
    private ArraySet(List<E> unmodifiableOrderedList, Comparator<? super E> comparator)
    /* requires(is_unmodifiable_v<decltype(unmodifiableList)>
    // ok that's unreal && is_ordered_v<decltype(unmodifiableOrderedList)>) */ {
        cmp = comparator;
        list = unmodifiableOrderedList;
    }
    
    public ArraySet(ArraySet<E> other) {
        cmp = other.cmp;
        list = other.list;
    }
    
    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        cmp = comparator;
        list = Collections.unmodifiableList(makeUnique(collection));
    }
    
    @Override
    public E lower(E e) {
        return lowerElement(e, false);
    }
    
    private E lowerElement(E e, boolean inclusive) {
        var index = lowerIndex(e, inclusive);
        return index >= 0 ? list.get(index) : null;
    }
    
    private int lowerIndex(E e, boolean inclusive) {
        return getIndex(e, (inclusive ? 0 : -1), -2);
    }
    
    private int getIndex(E e, int addIfInside, int addIfAbsent) {
        var index = Collections.binarySearch(list, e, cmp);
        if (index >= 0) {
            index += addIfInside;
        } else {
            index = -index + addIfAbsent;
        }
        return index;
    }
    
    @Override
    public E floor(E e) {
        return lowerElement(e, true);
    }
    
    @Override
    public E ceiling(E e) {
        return upperElement(e, true);
    }
    
    private E upperElement(E e, boolean inclusive) {
        var index = upperIndex(e, inclusive);
        return index < size() ? list.get(index) : null;
    }
    
    private int upperIndex(E e, boolean inclusive) {
        return getIndex(e, (inclusive ? 0 : 1), -1);
    }
    
    @Override
    public E higher(E e) {
        return upperElement(e, false);
    }
    
    @Override
    public E pollFirst() {
        throw new UncheckedUnmodifiableClassException();
    }
    
    @Override
    public E pollLast() {
        throw new UncheckedUnmodifiableClassException();
    }
    
    @Override
    public ArraySet<E> descendingSet() {
        return new ArraySet<>(list.reversed(), Collections.reverseOrder(cmp));
    }
    
    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }
    
    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (actualCompare(fromElement, toElement) == 1) {
            throw new IllegalArgumentException("fromElement should be less or equal than toElement");
        }
        return unsafeSubSet(fromElement, fromInclusive, toElement, toInclusive);
    }
    
    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return isEmpty() ?
                Collections.emptyNavigableSet() :
                unsafeSubSet(getFirst(), true, toElement, inclusive);
    }
    
    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return isEmpty() ?
                Collections.emptyNavigableSet() :
                unsafeSubSet(fromElement, inclusive, getLast(), true);
    }
    
    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }
    
    private NavigableSet<E> unsafeSubSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int fromIndex = upperIndex(fromElement, fromInclusive);
        int toIndex = lowerIndex(toElement, toInclusive);
        return fromIndex <= toIndex ?
                new ArraySet<>(list.subList(fromIndex, toIndex + 1), comparator()) :
                Collections.emptyNavigableSet();
    }
    
    @Override
    public Comparator<? super E> comparator() {
        return cmp;
    }
    
    @Override
    public E first() {
        return list.getFirst();
    }

    @Override
    public E last() {
        return list.getLast();
    }
    
    @Override
    public Spliterator<E> spliterator() {
        return NavigableSet.super.spliterator();
    }
    
    @Override
    public void addFirst(E e) {
        throw new UncheckedUnmodifiableClassException();
    }
    
    @Override
    public void addLast(E e) {
        throw new UncheckedUnmodifiableClassException();
    }

    @Override
    public E getFirst() {
        return list.getFirst();
    }
    
    @Override
    public E getLast() {
        return list.getLast();
    }
    
    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }
    
    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }
    
    @Override
    public E removeFirst() {
        throw new UncheckedUnmodifiableClassException();
    }
    
    @Override
    public E removeLast() {
        throw new UncheckedUnmodifiableClassException();
    }
    
    @Override
    public int size() {
        return list.size();
    }
    
    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }
    

    @Override
    @SuppressWarnings("unchecked")
    public int indexOf(Object o) {
        int index = lowerIndex((E)o, true);
        if (index < 0) {
            return -1;
        }
        E result = get(index);
        return actualCompare((E)o, result) == 0 ? index : -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }
    
    @Override
    public ArraySet<E> reversed() {
        return descendingSet();
    }
}