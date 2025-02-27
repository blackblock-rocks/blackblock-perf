package rocks.blackblock.perf.thread;

import rocks.blackblock.bib.monitor.GlitchGuru;
import rocks.blackblock.perf.BlackblockPerf;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class ThreadPool {

    // The number of threads to use
    private final int thread_count;

    // The current executor
    private ThreadPoolExecutor executor;

    // The number of threads currently active
    private final IntLatch active_count = new IntLatch();

    // All the current threads
    private final List<Thread> threads = new ArrayList<>();

    // The name of this thread pool
    private final String name;

    // The threads and their busy state
    private final ConcurrentHashMap<Thread, Boolean> threads_busy = new ConcurrentHashMap<>();

    /**
     * Creates a thread pool with the number of available processors
     * @since 0.1.0
     */
    public ThreadPool(String name) {
        this(name, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates a thread pool with the given number of threads
     * @since 0.1.0
     */
    public ThreadPool(String name, int thread_count) {
        this.name = name;
        this.thread_count = thread_count;
        this.restart();
    }

    /**
     * Get the number of threads to use
     * @since 0.1.0
     */
    public int getThreadCount() {
        return this.thread_count;
    }

    /**
     * Get the number of threads currently active
     * @since 0.1.0
     */
    public int getActiveCount() {
        return this.active_count.getCount();
    }

    /**
     * Is this thread pool currently performing work?
     * @since 0.1.0
     */
    public boolean isActive() {
        return this.active_count.getCount() > 0;
    }

    /**
     * Get the executor
     * @since 0.1.0
     */
    public ThreadPoolExecutor getExecutor() {
        return this.executor;
    }

    /**
     * Execute a task on the thread pool
     * @since 0.1.0
     */
    public void execute(Runnable action) {
        this.active_count.increment();

        this.executor.execute(() -> {
            this.threads_busy.put(Thread.currentThread(), true);

            try {
                action.run();
            } catch (Throwable t) {
                GlitchGuru.registerThrowable(t, "ThreadPool");
                throw t;
            } finally {
                this.threads_busy.put(Thread.currentThread(), false);
                this.active_count.decrement();
            }
        });
    }

    /**
     * Execute a task in parallel for each element in the iterator
     * @since 0.1.0
     */
    public <V> void concurrentForEach(int threshold, Collection<V> collection, Consumer<V> action) {

        // If the collection size doesn't exceed the threshold, just iterate over it
        if (collection.size() < threshold) {
            collection.forEach(action);
            return;
        }

        final var spliterator = collection.spliterator();
        final var split = split(spliterator, this.getThreadCount());

        for (final var splitter : split) {
            this.execute(() -> {
                splitter.forEachRemaining(action);
            });
        }

        this.awaitCompletion();
    }

    /**
     * Execute a task in parallel for each element in the iterator
     * @since 0.1.0
     */
    public <V> void concurrentForEach(int threshold, Map<?, V> map, Consumer<V> action) {

        final var values = map.values();

        // If the map size doesn't exceed the threshold, just iterate over it
        if (values.size() < threshold) {
            values.forEach(action);
            return;
        }

        final var spliterator = values.spliterator();
        final var split = split(spliterator, this.getThreadCount());

        for (final var splitter : split) {
            this.execute(() -> {
                splitter.forEachRemaining(action);
            });
        }

        this.awaitCompletion();
    }

    /**
     * Execute a task in parallel for each element in the iterator.
     * Afterwards, each successful task will have a cleanup action executed on the main thread.
     *
     * @since 0.1.0
     */
    public <V, R> void concurrentForEachWithLocalCleanup(List<V> values, Function<V, R> task, Consumer<R> cleanup) {

        if (values.isEmpty()) {
            return;
        }

        final var spliterator = values.spliterator();
        final var split = split(spliterator, this.getThreadCount());
        final var queue = Collections.synchronizedList(new ArrayList<R>(values.size()));

        for (final var splitter : split) {
            this.execute(() -> {
                splitter.forEachRemaining(value -> {
                    var result = task.apply(value);
                    if (result != null) {
                        queue.add(result);
                    }
                });
            });
        }

        this.awaitCompletion();

        for (var result : queue) {
            cleanup.accept(result);
        }
    }

    public <T> void execute(Iterator<T> iterator, Consumer<T> action) {
        iterator.forEachRemaining(t -> this.execute(() -> action.accept(t)));
    }

    public <T> void execute(Iterable<T> iterable, Consumer<T> action) {
        for (T t : iterable) {
            this.execute(() -> action.accept(t));
        }
    }

    public <T> void execute(Stream<T> stream, Consumer<T> action) {
        stream.forEach(t -> this.execute(() -> action.accept(t)));
    }

    public void execute(IntStream stream, IntConsumer action) {
        stream.forEach(t -> this.execute(() -> action.accept(t)));
    }

    public void execute(LongStream stream, LongConsumer action) {
        stream.forEach(t -> this.execute(() -> action.accept(t)));
    }

    public void execute(DoubleStream stream, DoubleConsumer action) {
        stream.forEach(t -> this.execute(() -> action.accept(t)));
    }

    public <T> void execute(T[] array, Consumer<T> action) {
        for(T t : array) this.execute(() -> action.accept(t));
    }

    public void execute(boolean[] array, Consumer<Boolean> action) {
        for(boolean t : array) this.execute(() -> action.accept(t));
    }

    public void execute(byte[] array, Consumer<Byte> action) {
        for(byte t : array) this.execute(() -> action.accept(t));
    }

    public void execute(short[] array, Consumer<Short> action) {
        for(short t : array) this.execute(() -> action.accept(t));
    }

    public void execute(int[] array, IntConsumer action) {
        for(int t : array) this.execute(() -> action.accept(t));
    }

    public void execute(long[] array, LongConsumer action) {
        for(long t : array) this.execute(() -> action.accept(t));
    }

    public void execute(float[] array, Consumer<Float> action) {
        for(float t : array) this.execute(() -> action.accept(t));
    }

    public void execute(double[] array, DoubleConsumer action) {
        for(double t : array) this.execute(() -> action.accept(t));
    }

    public void execute(char[] array, Consumer<Character> action) {
        for(char t : array) this.execute(() -> action.accept(t));
    }

    public void awaitFreeThread() {
        this.waitFor(value -> value < this.getThreadCount());
    }

    /**
     * Wait until all tasks are completed
     * @since 0.1.0
     */
    public void awaitCompletion() {
        this.waitFor(value -> value == 0);
    }

    public void waitFor(IntPredicate condition) {
        try {
            this.active_count.waitUntil(condition);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void restart() {
        if(this.executor == null || this.executor.isShutdown()) {
            this.threads.clear();
            this.threads_busy.clear();

            this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.thread_count, r -> {
                Thread t  = new Thread(r);
                t.setDaemon(true);
                t.setName(BlackblockPerf.MOD_ID + "_" + this.name + "_" + "unassigned");
                this.threads.add(t);
                return t;
            });
        }
    }

    /**
     * Drain the queues of all the inactive threads
     * @since 0.1.0
     */
    public void drainInactiveThreadQueues() {
        for (var thread : this.threads) {
            if (!this.threads_busy.getOrDefault(thread, true)) {
                DynamicThreads.drainThreadQueue(thread);
            }
        }
    }

    public void shutdown() {
        this.executor.shutdown();
    }

    public boolean isShutdown() {
        return this.executor.isShutdown();
    }

    private static class IntLatch {
        private CountDownLatch latch;

        private IntLatch() {
            this(0);
        }

        private IntLatch(int count) {
            this.latch = new CountDownLatch(count);
        }

        private synchronized int getCount() {
            return (int)this.latch.getCount();
        }

        private synchronized void decrement() {
            this.latch.countDown();
            this.notifyAll();
        }

        private synchronized void increment() {
            this.latch = new CountDownLatch((int)this.latch.getCount() + 1);
            this.notifyAll();
        }

        private synchronized void waitUntil(IntPredicate predicate) throws InterruptedException {
            while(!predicate.test(this.getCount())) {
                this.wait();
            }
        }
    }

    private static <V> Collection<Spliterator<V>> split(Spliterator<V> spliterator, int amount) {
        final var list = new ArrayList<Spliterator<V>>(amount);
        list.add(spliterator);


        for (int i = 0, a = 1, c = 1; c < amount; i++, c++) {
            if (i == a) {
                a += a;
                i = 0;
            }

            final var sub = list.get(i).trySplit();
            if (sub == null) break;
            list.add(sub);
        }

        return list;
    }
}