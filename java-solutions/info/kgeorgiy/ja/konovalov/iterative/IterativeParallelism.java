package info.kgeorgiy.ja.konovalov.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.*;

/**
 * IterativeParallelism class provides parallel execution of various list operations.
 */
public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;
    
    /**
     * Default constructs IterativeParallelism
     */
    public IterativeParallelism() {
        parallelMapper = null;
    }
    
    /**
     * Constructs IterativeParallelism from given mapper
     */
    public IterativeParallelism(ParallelMapper mapper) {
        parallelMapper = mapper;
    }
    

    /**
     * Record that represents mapped monoid
     *
     * @param neutralSupplier function that returns new instance of neutral element
     * @param adder           function that returns newState from previousState and nextElement
     * @param <T>             Type of elements processed
     * @param <V>             Type of internal state
     */
    private static record Operation<T, V>(Supplier<V> neutralSupplier, BiFunction<V, T, V> adder) {
    }

    /**
     * Basically a mapper(T -> V) + monoid over V
     *
     * @param <T> type for elements that are proceeded
     * @param <V> type for internal state
     */
    // could actually implement as State<T> but that sometimes is not applicable,
    // in {@link #join} for example, creating extra time StringBuilders is bad for performance
    private static class State<T, V> {
        private final Operation<T, V> operation;
        public V currentState;

        State(final Operation<T, V> operation) {
            this.currentState = operation.neutralSupplier.get();
            this.operation = operation;
        }

        public void apply(final T nxt) {
            currentState = operation.adder.apply(currentState, nxt);
        }
    }
    
    private static record StateAndValuesRecord<T, V>(State<T, V> state, List<? extends T> values) {
    }


    /**
     * Executes the given operation on a list of values in parallel using multiple threads.
     *
     * @param threads           the number of concurrent threads to use
     * @param values            the list of values to process
     * @param operation         the operation that is performed on elements
     * @param resultsCombinator the combiner function to combine the results of different threads
     * @param <T>               the type of values in the list
     * @param <V>               the type of the operation's result
     * @return the combined result of all the operations
     * @throws InterruptedException if any of the threads is interrupted during execution
     */
    private <T, V> V parallelize(int threads, List<? extends T> values, Operation<T, V> operation, BinaryOperator<V> resultsCombinator) throws InterruptedException {
        int blockSize = values.size() / threads;
        int reminder = values.size() % threads;
        List<StateAndValuesRecord<T, V>> states = new ArrayList<>();
        
        for (int i = 0; i < threads; i++) {
            // :NOTE: все еще считаю, что State и StateAndValuesRecord на самом деле особо не нужно
            states.add(new StateAndValuesRecord<>(new State<>(operation), getNthBlockList(values, reminder, blockSize, i)));
        }
        
        if (parallelMapper != null) {
            return parallelMapper.map((e) ->  {
                for (var u : e.values) {
                    e.state.apply(u);
                }
                return e.state.currentState;
            }, states).stream().reduce(operation.neutralSupplier.get(), resultsCombinator);
        } else {
            final List<Thread> runningThreads = new ArrayList<>();
            for (int i = 0; i < states.size(); ++i) {
                final int finalI = i;
                runningThreads.add(new Thread(() -> {
                    for (var u : states.get(finalI).values) {
                        states.get(finalI).state.apply(u);
                    }
                }));
                runningThreads.getLast().start();
            }
            
            InterruptedException throwExceptionDuringJoins = null;
            for (Thread runningThread : runningThreads) {
                try {
                    runningThread.join();
                } catch (InterruptedException exception) {
                    if (throwExceptionDuringJoins == null) {
                        throwExceptionDuringJoins = exception;
                    } else {
                        throwExceptionDuringJoins.addSuppressed(exception);
                    }
                    // :NOTE: если вылетает исключение, мы не дожидаемя завершени я потока
                }
            }
            
            if (throwExceptionDuringJoins != null) {
                throw throwExceptionDuringJoins;
            }

            // :NOTE: neutralSupplier не нужен
            return states.stream()
                         .map(x -> x.state.currentState)
                         .reduce(operation.neutralSupplier.get(), resultsCombinator);
        }
    }

    /**
     * Returns a sub-list of elements from the specified list that belong to the nth block.
     *
     * @param <T>         the type of elements in the list
     * @param list        the list of elements
     * @param reminder    the result of list.size() % threadsNumber, used to determine bounds
     * @param blockSize   the size of each block
     * @param blockNumber the index of the block to retrieve
     * @return a sub-list of elements from the specified list that belong to the nth block
     */
    private static <T> List<? extends T> getNthBlockList(List<? extends T> list, int reminder, int blockSize, int blockNumber) {
        int lowerBound = blockSize * blockNumber + Integer.min(blockNumber, reminder);
        int upperBound = lowerBound + blockSize + (blockNumber < reminder ? 1 : 0);
        return list.subList(lowerBound, upperBound);
    }
    
    private static <T> List<? extends T> wrapToSteps(List<? extends T> values, int step) {
        return step == 1 ? values : new SteppedList<>(values, step);
    }
    
    
    @Override
    public String join(int threads, List<?> values, int step) throws InterruptedException {
        // :NOTE: можно использовать стримы и Collections.joining()
        return genericMapReduce(threads, values, (a) -> new StringBuilder(a.toString()),
                                StringBuilder::new, StringBuilder::append, step).toString();
    }
    
    /*
    * Only two methods that are huge: filter and map
    * */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return genericMapReduceWithAdder(threads, values, ArrayList::new, (a, b) -> {
            if (predicate.test(b)) {
                a.add(b);
            }
            return a;
        }, (a, b) -> {
            a.addAll(b);
            return a;
        }, step);
    }
    
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f, int step) throws InterruptedException {
        return genericMapReduceWithAdder(threads, values, ArrayList::new, (a, b) -> {
            a.add(f.apply(b));
            return a;
        }, (a, b) -> {
            a.addAll(b);
            return a;
        }, step);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return maximum(threads, values, comparator.reversed(), step);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return genericMapReduce(threads, values, predicate::test, () -> true, (a, b) -> a & b, step);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return genericMapReduce(threads, values, predicate::test, () -> false, (a, b) -> a | b, step);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate, int steps) throws InterruptedException {
        return genericMapReduce(threads, values, (a) -> predicate.test(a) ? 1 : 0, () -> 0, Integer::sum, steps);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator, int steps) throws InterruptedException {
        return genericReduce(threads, values, values::getFirst, (T a, T b) -> comparator.compare(a, b) < 0 ? b : a, steps);
    }
    
    
    private <T> T genericReduce(int threads, List<? extends T> values, Supplier<T> identitySupplier,
                               BinaryOperator<T> operator, int step) throws InterruptedException {
        // :NOTE: вижу много wrapToSteps(values, step)
        return parallelize(threads, wrapToSteps(values, step), new Operation<>(identitySupplier, operator), operator);
    }
    
    private <T, R> R genericMapReduceWithAdder(int threads, List<? extends T> values,
                                     Supplier<R> identitySupplier, BiFunction<R, T, R> adder, BinaryOperator<R> operator, int step) throws InterruptedException {
        return parallelize(threads, wrapToSteps(values, step), new Operation<>(identitySupplier, adder), operator);
    }
    
    private <T, R> R genericMapReduce(int threads, List<? extends T> values, Function<? super T, ? extends R> lift,
                                        Supplier<R> identitySupplier, BinaryOperator<R> operator, int step) throws InterruptedException {
        return genericMapReduceWithAdder(threads, values, identitySupplier,
                                         (a, b) -> operator.apply(a, lift.apply(b)), operator, step);
    }
    
    @Override
    public <T> T reduce(int threads, List<T> values, T identity, BinaryOperator<T> operator, int step)
            throws InterruptedException {
        return genericReduce(threads, values, () -> identity, operator, step);
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, R identity, BinaryOperator<R> operator, int step)
            throws InterruptedException {
        return genericMapReduce(threads, values, lift, () -> identity, operator, step);
    }
}

