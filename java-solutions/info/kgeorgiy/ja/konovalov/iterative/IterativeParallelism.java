package info.kgeorgiy.ja.konovalov.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Constructs IterativeParallelism from given valuesTransformer
     */
    public IterativeParallelism(ParallelMapper mapper) {
        parallelMapper = mapper;
    }
    
    
    /**
     * Represents an operation(with neutral elements, so basically a monoid-like operation)
     * that can be performed on a stream of elements.
     *
     * @param <T> the type of the input elements of the operation
     * @param <V> the type of the result of the operation
     */
    private static class Operation<T, V> {
        /**
         * Represents a supplier of results with a neutral value. Used
         *
         * @see Operation
         */
        public final Supplier<V> neutralSupplier;
        /**
         * Represents a function that applies operation on a stream of elements.
         * Designed to be used as mapReduce function most of the cases
         */
        public final Function<Stream<? extends T>, V> valuesTransformer;
        /**
         * Represents a function that applies a reducing operation on a stream of answers.
         * So that is used in aggregating final answer
         */
        public final Function<Stream<V>, V> resultsCombiner;
        
        /**
         * Creates Operation from given supplier, mapper and results-combiner
         * Represents an operation (with neutral elements, so basically a monoid-like operation)
         * that can be performed on a stream of elements.
         */
        public Operation(Supplier<V> neutralSupplier, Function<Stream<? extends T>, V> mapReducer, Function<Stream<V>, V> resultsCombiner) {
            this.neutralSupplier = neutralSupplier;
            this.valuesTransformer = mapReducer;
            this.resultsCombiner = resultsCombiner;
        }
    }
    
    /**
     * Executes the given operation on a list of values in parallel using multiple threads.
     *
     * @param threads           the number of concurrent threads to use
     * @param values            the list of values to process
     * @param operation         the operation that is performed on elements
     * @param <T>               the type of values in the list
     * @param <V>               the type of the operation's result
     * @return the combined result of all the operations
     * @throws InterruptedException if any of the threads is interrupted during execution
     */
    private <T, V> V parallelize(int threads, List<? extends T> values, Operation<T, V> operation) throws InterruptedException {
        
        // :NOTE: все еще считаю, что State и StateAndValuesRecord на самом деле особо не нужно
        // :NOTE-ANSWER: убрал и State и StateAndValuesRecord
        
        final List<List<? extends T>> blocks = new ArrayList<>();
        final List<V> answers = new ArrayList<>();
        
        for (int i = 0; i < threads; i++) {
            blocks.add(getNthBlockList(values, values.size() % threads, values.size() / threads, i));
            answers.add(operation.neutralSupplier.get());
        }
        
        if (parallelMapper != null) {
            return operation.resultsCombiner.apply(parallelMapper.map((e) -> operation.valuesTransformer.apply(values.stream()), blocks).stream());
        } else {
            final List<Thread> runningThreads = new ArrayList<>();
            for (int i = 0; i < blocks.size(); ++i) {
                final int finalI = i;
                runningThreads.add(new Thread(() ->
                    answers.set(finalI, operation.valuesTransformer.apply(blocks.get(finalI).stream()))
                ));
                runningThreads.getLast().start();
            }
            
            InterruptedException throwExceptionDuringJoins = null;
            for (Thread runningThread : runningThreads) {
                if (throwExceptionDuringJoins != null) {
                    // to speed up the joining, as we don't care about the computation result anymore
                    runningThread.interrupt();
                }
                
                boolean succeeded = false;
                while(!succeeded) {
                    try {
                        runningThread.join();
                        succeeded = true;
                    } catch (InterruptedException exception) {
                        if (throwExceptionDuringJoins == null) {
                            throwExceptionDuringJoins = exception;
                        } else {
                            throwExceptionDuringJoins.addSuppressed(exception);
                            
                            // to speed up the joining, as we don't care about the computation result anymore
                            runningThread.interrupt();
                        }
                        // :NOTE: если вылетает исключение, мы не дожидаемя завершени я потока
                        // :NOTE-ANSWER: теперь не вылетаем, а делаем join до победного
                    }
                }
            }
            
            if (throwExceptionDuringJoins != null) {
                throw throwExceptionDuringJoins;
            }
            
            return operation.resultsCombiner.apply(answers.stream());
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
    
    
    // :NOTE: вижу много wrapToSteps()
    // :NOTE-ANSWER: Я и в прошлый раз то не понял почему много(ровно 2 раза было, но в похожих функциях -- reduce и mapReduce)
    // Но, во-первых, нет вложенных, а во-вторых, ровно в двух местах написан wrapToSteps(),
    // ровно в двух местах, где нужно явно вызвать parallelize, всего я разделил операции на два типа:
    // genericMapReduce где нужно сделать MapReduce просто с какими-то понятными операциями map и потом reduce
    // collectorsOperation где я явно использую два коллектора, для mapping'а и для reduce'а
    // Кажется ровно два wrapToSteps() это как раз норм... Не понимаю что не так было с их количеством
    
    @Override
    public String join(int threads, List<?> values, int step) throws InterruptedException {
        // :NOTE: можно использовать стримы и Collections.joining()
        // :NOTE-ANSWER: сделал
        return collectorsOperation(threads, values, step,
                                   String::new,
                                   Collectors.mapping(Object::toString, Collectors.joining()),
                                   Collectors.joining()
        );
    }
    
    private <T, U> U collectorsOperation(int threads, List<? extends T> values, int step,
                                                        Supplier<U> identitySupplier,
                                                        Collector<? super T, ?, U> mappingCollector,
                                                        Collector<? super U, ?, U> resultsCollector) throws InterruptedException {
        return parallelize(threads, wrapToSteps(values, step),
                           new Operation<>(
                                   identitySupplier,
                                   (Stream<? extends T> s) -> s.collect(mappingCollector),
                                   (s -> s.collect(resultsCollector))
        ));
    }
    
    
    private <T, U, A> List<U> listTransformingOperation(int threads, List<? extends T> values, int step,
                                                        Collector<T, A, List<U>> collector) throws InterruptedException {
        return collectorsOperation(threads, values, step, ArrayList::new, collector,
                Collectors.flatMapping(List::stream, Collectors.toList())
        );
    }
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return listTransformingOperation(threads, values, step, Collectors.filtering(predicate, Collectors.toCollection(ArrayList::new)));
    }
    
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> mapper, int step) throws InterruptedException {
        return listTransformingOperation(threads, values, step, Collectors.mapping(mapper, Collectors.toCollection(ArrayList::new)));
    }
    
    private <T, R> R genericMapReduce(int threads, List<? extends T> values, Function<T, R> mapper,
                                      Supplier<R> identitySupplier, BinaryOperator<R> operator, int step) throws InterruptedException {
        return parallelize(threads, wrapToSteps(values, step),
                           new Operation<>(
                                   identitySupplier,
                                   (Stream<? extends T> stream) -> stream.map(mapper).reduce(identitySupplier.get(), operator),
                                   s -> s.reduce(identitySupplier.get(), operator)
                           )
        );
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
        return genericMapReduce(threads, values, Function.identity(), values::getFirst, (T a, T b) -> comparator.compare(a, b) < 0 ? b : a, steps);
    }
    
    @Override
    public <T> T reduce(int threads, List<T> values, T identity, BinaryOperator<T> operator, int step) throws InterruptedException {
        return genericMapReduce(threads, values, Function.identity(),  () -> identity, operator, step);
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, R identity, BinaryOperator<R> operator, int step) throws InterruptedException {
        return genericMapReduce(threads, values, lift, () -> identity, operator, step);
    }
}

