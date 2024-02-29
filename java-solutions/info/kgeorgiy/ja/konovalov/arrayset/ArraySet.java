package info.kgeorgiy.ja.konovalov.arrayset;

import java.util.*;

import static java.lang.Integer.min;

public class ArraySet<E> extends java.util.AbstractSet<E> implements NavigableSet<E> {
    
    private final Comparator<? super E> cmp;
    private final List<E> list;
    
    public ArraySet() {
        cmp = null;
        list = java.util.Collections.emptyList();
    }
    
    public ArraySet(Collection<? extends E> collection) {
        cmp = null;
        list = List.copyOf(makeUnique(collection));
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
        list = java.util.Collections.emptyList();
    }
    
    //ah, old good semantics by naming... One day I hope there will be templates, not generics.
    private ArraySet(List<E> unmodifiableList, Comparator<? super E> comparator)
    /* requires(is_unmodifiable_v<decltype(unmodifiableList)>) */ {
        cmp = comparator;
        list = unmodifiableList;
    }
    
    public ArraySet(ArraySet<E> other) {
        cmp = other.cmp;
        list = other.list;
    }
    
    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        cmp = comparator;
        list = List.copyOf(makeUnique(collection));
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
        var index = java.util.Collections.binarySearch(list, e, cmp);
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
        prohibitPolling();
        return null;
    }
    
    private void prohibitPolling() {
        throw new UnsupportedOperationException("polling from unmodifiable set is unsupported");
    }
    
    @Override
    public E pollLast() {
        prohibitPolling();
        return null;
    }
    
    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(list.reversed(), Collections.reverseOrder(cmp));
    }
    
    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }
    
    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (actualCompare(fromElement, toElement) == 1) {
            throw new IllegalArgumentException("fromElement should be less than toElement");
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
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }
    
    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }
    
    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }
    
    @Override
    public int size() {
        return list.size();
    }
    
    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        E actualElement = (E) o;
        var index = lowerIndex(actualElement, true);
        return index >= 0 && actualCompare(list.get(index), actualElement) == 0;
    }
    
    @Override
    public Object[] toArray() {
        return list.toArray();
    }
    
    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }
}