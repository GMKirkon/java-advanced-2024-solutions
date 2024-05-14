package info.kgeorgiy.ja.konovalov.iterative;


import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ParallelMapperImpl implements ParallelMapper {
    private volatile boolean closed = false;
    private final static int DEFAULT_TIMEOUT_IN_MILLISECONDS = 1000;
    private final List<Thread> runningThreads;
    
    private final SimplifiedSynchronizedQueue<Runnable> queue;
    
    
    /**
     * Creates a new instance of the ParallelMapperImpl class with the specified number of threads.
     * Number of threads should be greater than zero.
     * Each thread dequeues a task from the shared queue and executes it until the thread is interrupted.
     *
     * @param threads the number of threads to be created
     * @throws IllegalArgumentException if the number of threads is non-positive
     */
    public ParallelMapperImpl(int threads) {
        this.queue = new SimplifiedSynchronizedQueue<>();
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads should be positive");
        }
        
        runningThreads = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            runningThreads.add(new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted() && !closed) {
                        var task = queue.poll();
                        if (task != null) {
                            task.run();
                        }
                    }
                } catch (InterruptedException ignored) {
                    // just ignore while main thread is not interrupted
                }
            }));
            runningThreads.get(i).start();
        }
    }
    
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        ensureOpen();
        
        MapQueryHelper<R> currentHelper = new MapQueryHelper<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            int finalI = i;
            queue.push(() -> {
                try {
                    R result = f.apply(args.get(finalI));
                    currentHelper.set(finalI, result);
                } catch (RuntimeException e) {
                    currentHelper.addSuppressed(e);
                }
            });
        }
        return currentHelper.evaluate();
    }
    
    @Override
    public void close() {
        ensureOpen();
        
        closed = true;
        queue.terminate();
        runningThreads.forEach(Thread::interrupt);
        for (var u : runningThreads) {
            boolean succeeded = false;
            while (!succeeded) {
                try {
                    u.join();
                    succeeded = true;
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
    
    /**
     * Checks that ParallelMapper is not closed
     */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("ParallelMapper is closed");
        }
    }
    
    /**
     * Class provides a helper for parallel mapping operations.
     * Stores {@code List<R>} that is going to be returned as a result of map operation.
     * Exception aggregated from all mapping operations and
     * Counter that shows how many elements are not processed yet
     * @param <R>
     */
    private class MapQueryHelper<R> {
        private final List<R> mappedValues;
        private final SynchronizedValue<RuntimeException> cumulativeException;
        private final CounterDown counter;
        
        /**
         * Creates MapQueryHelper with list of proviede capacity
         */
        public MapQueryHelper(int capacity) {
            counter = new CounterDown(capacity);
            cumulativeException = new SynchronizedValue<>(null);
            mappedValues = new ArrayList<>(Collections.nCopies(capacity, null));
        }
        
        /**
         * Sets the mapped value at the specified index in the mappedValues list, UN_SYNCHRONIZED!
         * By the contract only one set to each index is allowed
         * Decreases the counter SYNCHRONIZED.
         *
         * @param index       the index at which to set the mapped value
         * @param mappedValue the mapped value to be set to the resulting list
         */
        public void set(int index, R mappedValue) {
            mappedValues.set(index, mappedValue);
            decreaseCounter();
        }
        
        /**
         * Adds the specified exception to current aggregated exception
         *
         * @param e the exception to add to the suppressed exceptions list
         */
        public void addSuppressed(RuntimeException e) {
            cumulativeException.apply(currentException -> {
                if (currentException == null) {
                    return e;
                } else {
                    currentException.addSuppressed(e);
                    return currentException;
                }
            });
            decreaseCounter();
        }
        
        //synchronized is only needed to be able to do notify, so for that reason counter is not synchronized,
        // and just counts down under locks :(, would be so nice to make it SynchronizedValue too, like the exception
        /**
         * Decreases the counter. If the counter reaches zero after decrementing, it notifies wait inside {@link #evaluate()}
         */
        synchronized private void decreaseCounter() {
            if (counter.decrementAndCheckForZero()) {
                notify();
            }
        }
        
        /**
         * This method evaluates the results of a parallel mapping operation.
         *
         * @return the list of mapped values
         * @throws InterruptedException if the current thread is interrupted while waiting
         * @throws IllegalStateException if the parallel mapper is closed
         * @throws RuntimeException if any exceptions were thrown during the mapping operation
         */
        synchronized public List<R> evaluate() throws InterruptedException {
            while (!counter.isZero() && !closed) {
                wait(DEFAULT_TIMEOUT_IN_MILLISECONDS);
            }
            
            //even if counted correctly, if mapper is closed exception is thrown :(
            if (closed) {
                ensureOpen();
            }
            
            RuntimeException e = cumulativeException.get();
            if (e == null) {
                return mappedValues;
            } else {
                throw e;
            }
        }
    }
    
    /**
     * CounterDown represents a counter that can be decremented and checked for zero.
     * The counter cannot have a negative value.
     * <h3>Consider using java.util.concurrent.CountDownLatch</h3>
     * @see java.util.concurrent.CountDownLatch
     */
    private static class CounterDown {
        /**
         * Stored counter
         */
        private int counter;
        
        /**
         * Constructs Counter with given number of decreases to perform
         * @param counter timer value to set
         */
        CounterDown(int counter) {
            if (counter < 0) {
                throw new IllegalArgumentException("counter should be used with non-negative values");
            }
            this.counter = counter;
        }
        
        /**
         * Decrements counter, returns true if counter was decremented to zero.
         * @return true if counter is equal to 0 after decrement, false otherwise
         */
        boolean decrementAndCheckForZero() {
            counter--;
            return counter == 0;
        }
        
        /**
         * Checks that stored value is zero
         * @return true if counter is zero, false otherwise
         */
        boolean isZero() {
            return counter == 0;
        }
    }
    
    /**
     * A thread-safe wrapper class that holds a value and provides synchronized methods to modify and access the value.
     * Implemented with mutexes... So not good for performance.
     * <h3>
     * consider using Atomics if you have such option
     * </h3>
     * @param <T> the type of the value
     */
    private static class SynchronizedValue<T> {
        /**
         * Stored value
         */
        private T value;
        
        /**
         * Default construct SynchronizedValue from given Value
         */
        public SynchronizedValue(T value) {
            this.value = value;
        }
        
        /**
         * Applies the given unary operator to the value and sets value as a result
         *
         * @param f the unary operator to apply
         */
        synchronized void apply(UnaryOperator<T> f) {
            value = f.apply(value);
        }
        
        /**
         * Returns stored value. Should be called at most once.
         * Double invocation of that function is undefined behaviour
         * @return stored value
         */
        synchronized T get() {
            return value;
        }
    }
    
    
    /**
     * SimplifiedSynchronizedQueue is a thread-safe implementation of a significantly simplified version queue,
     * with ONLY synchronized methods push and poll, no iterators, spliterators and many other methods
     * of regular queue
     * <h3>
     * Consider using lock-free or better locking queue instead if you have such option
     * </h3>
     * @param <T> the type of elements in the queue
     */
    private class SimplifiedSynchronizedQueue<T> {
        private final Queue<T> queue;
        private final int MAXIMUM_CAPACITY = 10000;
        
        /**
         * Default constructs SimplifiedSynchronizedQueue
         */
        public SimplifiedSynchronizedQueue() {
            queue = new ArrayDeque<>(MAXIMUM_CAPACITY);
        }
        
        /**
         * Pushes an item into the queue.
         *
         * @param item the item to be pushed into the queue
         */
        public synchronized void push(final T item) throws InterruptedException {
            if (closed) {
                return;
            }
            while (queue.size() == MAXIMUM_CAPACITY) {
                wait();
            }
            queue.add(item);
            notifyAll();
        }
        
        /**
         * Retrieves and removes the element at the head of this queue,
         * waiting if necessary until an element becomes available.
         *
         * @return the head of this queue
         * @throws InterruptedException if the current thread is interrupted
         */
        public synchronized T poll() throws InterruptedException {
            if (closed) {
                return null;
            }
            while (queue.isEmpty()) {
                wait();
            }
            T value = queue.poll();
            notifyAll();
            return value;
        }
        
        /**
         * Clears the queue if parallel mapper was closed
         */
        public synchronized void terminate() {
            if (!closed) {
                return;
            }
            queue.clear();
        }
    }
}
