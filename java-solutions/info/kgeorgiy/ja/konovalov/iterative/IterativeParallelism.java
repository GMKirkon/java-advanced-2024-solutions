package info.kgeorgiy.ja.konovalov.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.*;

/**
 * IterativeParallelism class provides parallel execution of various list operations.
 */
public class IterativeParallelism implements AdvancedIP {

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
    private static <T, V> V parallelize(int threads, List<? extends T> values, Operation<T, V> operation, BinaryOperator<V> resultsCombinator) throws InterruptedException {
        int blockSize = values.size() / threads;
        List<State<T, V>> states = new ArrayList<>();
        List<Thread> createdThreads = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int threadIndex = i;
            states.add(new State<>(operation));
            createdThreads.add(new Thread(() -> processBlock(
                    getNthBlockList(values, threads, blockSize, threadIndex),
                    states.get(threadIndex)
            )));
            createdThreads.getLast().start();
        }

        for (var u : createdThreads) {
            u.join();
            // :NOTE: этот код не дожидается завершения исполнения всех потоков, если вылетает InterruptedExcpetion
        }

        return states.stream()
                .map(state -> state.currentState)
                .reduce(resultsCombinator)
                .get();
        // :NOTE: может, использовать .reduce(operation.neutralSupplier.get(),resultsCombinator), чтобы не пораждать промежуточные опшаналы
    }

    /**
     * Processes each value in the given list using the provided block state.
     *
     * @param <T>        type of values in list
     * @param <V>        type of internal state
     * @param values     the list of values to process
     * @param blockState the block state to apply on each value
     */
    private static <T, V> void processBlock(List<? extends T> values, State<T, V> blockState) {
        values.forEach(blockState::apply);
    }

    /**
     * Returns a sub-list of elements from the specified list that belong to the nth block.
     *
     * @param <T>         the type of elements in the list
     * @param list        the list of elements
     * @param threads     the number of concurrent threads to use
     * @param blockSize   the size of each block
     * @param blockNumber the index of the block to retrieve
     * @return a sub-list of elements from the specified list that belong to the nth block
     */
    private static <T> List<? extends T> getNthBlockList(List<? extends T> list, int threads, int blockSize, int blockNumber) {
        int lowerBound = blockSize * blockNumber;
        int upperBound = (blockNumber + 1 == threads) ? list.size() : lowerBound + blockSize;
        // :NOTE: не равномерное распределение задач по потокам: последнему потоку может достаться значтьельно меньше задач
        return list.subList(lowerBound, upperBound);
    }

    @Override
    public String join(int threads, List<?> values, int step) throws InterruptedException {
        return parallelize(threads, new SteppedList<>(values, step), new Operation<>(
                        StringBuilder::new,
                        (a, b) -> {
                            a.append(b.toString());
                            return a;
                        }
                ),
                (a, b) -> {
                    a.append(b.toString());
                    return a;
                }
        ).toString();
    }

    // :NOTE: идея с использованием своего Operation и State хорошая,
    // но можно написать значительно проще и короче, если разделить применение всех операций на преобразующие (map) операции и объединяющие результаты (reduce) операции
    // :NOTE: это позволяет реализовать все методы в 1-2 строки
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return parallelize(
                threads,
                new SteppedList<>(values, step),
                new Operation<T, List<T>>(
                        ArrayList::new,
                        (a, b) -> {
                            if (predicate.test(b)) {
                                a.add(b);
                            }
                            return a;
                        }
                ),
                (a, b) -> {
                    a.addAll(b);
                    return a;
                }
        );
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f, int step) throws InterruptedException {
        // :NOTE: если step == 1, вероятно, разумно не создавать промежуточный лист
        return parallelize(threads, new SteppedList<>(values, step), new Operation<T, List<U>>(
                        ArrayList::new,
                        (a, b) -> {
                            a.add(f.apply(b));
                            return a;
                        }
                ),
                (a, b) -> {
                    a.addAll(b);
                    return a;
                }
        );
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return maximum(threads, values, comparator.reversed(), step);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return parallelize(threads, new SteppedList<>(values, step), new Operation<T, Boolean>(
                        () -> true,
                        (a, b) -> predicate.test(b) && a
                ),
                (a, b) -> a & b
        );
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return parallelize(threads, new SteppedList<>(values, step), new Operation<T, Boolean>(
                        () -> false,
                        (a, b) -> predicate.test(b) || a
                ),
                (a, b) -> a | b
        );
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate, int steps) throws InterruptedException {
        return parallelize(threads, new SteppedList<>(values, steps), new Operation<T, Integer>(
                        () -> 0,
                        (a, b) -> {
                            if (predicate.test(b)) {
                                a += 1;
                            }
                            return a;
                        }
                ),
                Integer::sum
        );
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator, int steps) throws InterruptedException {
        return parallelize(
                threads,
                new SteppedList<>(values, steps),
                new Operation<T, Optional<T>>(
                        Optional::empty,
                        (a, b) -> {
                            if (a.isEmpty() || comparator.compare(a.get(), b) < 0) {
                                return Optional.of(b);
                            } else {
                                return a;
                            }
                        }
                ),
                (a, b) -> {
                    if (a.isEmpty()) {
                        return b;
                    }
                    if (b.isEmpty()) {
                        return a;
                    }
                    return comparator.compare(a.get(), b.get()) >= 0 ? a : b;
                }
        ).get();
    }

    @Override
    public <T> T reduce(int threads, List<T> values, T identity, BinaryOperator<T> operator, int step) throws InterruptedException {
        return parallelize(threads,
                new SteppedList<>(values, step),
                new Operation<>(() -> identity, operator),
                operator);
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, R identity, BinaryOperator<R> operator, int step) throws InterruptedException {
        return parallelize(
                threads,
                new SteppedList<>(values, step),
                new Operation<>(() -> identity, (a, b) -> operator.apply(a, lift.apply(b))),
                operator
        );
    }
}

