package rocks.blackblock.perf.thread;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import rocks.blackblock.bib.bv.parameter.IntegerParameter;
import rocks.blackblock.bib.bv.parameter.MapParameter;
import rocks.blackblock.bib.bv.value.BvInteger;
import rocks.blackblock.bib.monitor.GlitchGuru;
import rocks.blackblock.bib.runnable.Pledge;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.perf.BlackblockPerf;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * The main Dynamic Threads class
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class DynamicThreads {

	// The starting thread name
	public static final String THREAD_NAME_PREFIX = BlackblockPerf.MOD_ID + "_server_";

	// How many threads to currently use
	public static int THREADS_COUNT = 0;

	// If we're using threads at all
	public static boolean THREADS_ENABLED = false;

	// The current thread pool
	public static ThreadPool THREAD_POOL = null;

	// The "threading" tweak map
	public static final MapParameter<?> THREADING_TWEAKS = BlackblockPerf.createTweakMap("threading");

	// The TweakParameter to use for setting the amount of threads
	private static final IntegerParameter THREADS_PARAMETER = THREADING_TWEAKS.add(new IntegerParameter("max_threads"));

	// Thread busy state
	private static final ConcurrentHashMap<Thread, Boolean> THREAD_IS_BUSY = new ConcurrentHashMap<>();

	// Thread queues
	private static final ConcurrentHashMap<Thread, ConcurrentLinkedQueue<Runnable>> THREAD_QUEUES = new ConcurrentHashMap<>();


	/**
	 * Initialize the settings
	 * @since    0.1.0
	 */
	@ApiStatus.Internal
	public static void init() {

		// Don't use threads by default
		THREADS_PARAMETER.setDefaultValue(BvInteger.of(0));

		// Listen to changes to the thread count
		THREADS_PARAMETER.addChangeListener(bvIntegerChangeContext -> {
			int thread_count = bvIntegerChangeContext.getValue().getFlooredInteger();
			DynamicThreads.setNewThreadCount(thread_count);
		});
	}

	/**
	 * Temporarily swaps the main thread of given objects to the current thread,
	 * executes a task, and then restores the original threads.
	 * @since    0.1.0
	 */
	public static void swapThreadsAndRun(Runnable task, WithMutableThread... threaded_instances) {
		Thread new_thread = Thread.currentThread();
		Thread[] original_threads = new Thread[threaded_instances.length];

		for (int i = 0; i < original_threads.length; i++) {
			original_threads[i] = threaded_instances[i].bb$getMainThread();
			threaded_instances[i].bb$setMainThread(new_thread);
		}

		task.run();

		for (int i = 0; i < original_threads.length; i++) {
			threaded_instances[i].bb$setMainThread(original_threads[i]);
		}
	}

	/**
	 * Set the thread name
	 * @since    0.1.0
	 */
	public static void attachToThread(Thread thread, ServerWorld world) {
		attachToThread(thread, world.getRegistryKey().getValue().toString());
	}

	/**
	 * Set the thread name
	 * @since    0.1.0
	 */
	public static void attachToThread(Thread thread, String name) {
		thread.setName(THREAD_NAME_PREFIX + name);
		THREAD_IS_BUSY.put(thread, true);
	}

	/**
	 * Detach a thread
	 * @since    0.1.0
	 */
	public static void detachThread(Thread thread) {
		thread.setName(THREAD_NAME_PREFIX);
		THREAD_IS_BUSY.put(thread, false);
	}

	/**
	 * Is the given thread one of ours & busy?
	 * @since    0.1.0
	 */
	public static boolean ownsAndIsBusy(Thread thread) {
		return thread.getName().startsWith(THREAD_NAME_PREFIX) && THREAD_IS_BUSY.get(thread);
	}

	/**
	 * Is the given thread one of ours?
	 * @since    0.1.0
	 */
	public static boolean ownsThread(Thread thread) {
		return thread.getName().startsWith(THREAD_NAME_PREFIX);
	}

	/**
	 * Get the new amount of threads to use
	 * @since    0.1.0
	 */
	public static void setNewThreadCount(int new_thread_count) {

		if (THREADS_COUNT == new_thread_count) {
			return;
		}

		DynamicThreads.THREADS_COUNT = new_thread_count;
		DynamicThreads.THREADS_ENABLED = DynamicThreads.THREADS_COUNT > 0;

		BibLog.attention("Dimensional thread count:", DynamicThreads.THREADS_COUNT);

		if (DynamicThreads.THREADS_ENABLED) {
			DynamicThreads.THREAD_POOL = new ThreadPool("dimensions", DynamicThreads.THREADS_COUNT);

			BibPerf.setWorldInfoGetter(World::bb$getPerformanceInfo);
		} else {
			BibPerf.setWorldInfoGetter(null);
		}
	}

	/**
	 * Are we on the correct world thread?
	 *
	 * @since    0.1.0
	 */
	public static boolean onWorldThread(World world) {
		return Thread.currentThread() == world.bb$getMainThread();
	}

	/**
	 * The current thread is trying to change something belonging to another thread.
	 * We'll just hope that other thread belongs to us, and put something in its queue.
	 *
	 * @since    0.1.0
	 */
	public static <R> R ensureWorldThread(World world, Supplier<R> task) {

		Thread wanted_thread = world.bb$getMainThread();

		if (Thread.currentThread() == wanted_thread) {
			return task.get();
		}

		return waitOnOtherWorldThread(wanted_thread, task);
	}

	/**
	 * The current thread is trying to change something belonging to another thread.
	 * We'll just hope that other thread belongs to us, and put something in its queue.
	 *
	 * @since    0.1.0
	 */
	public static <R> R waitOnOtherWorldThread(Thread thread, Supplier<R> task) {

		var queue = THREAD_QUEUES.computeIfAbsent(thread, t -> new ConcurrentLinkedQueue<>());

		Pledge<R> pledge = new Pledge<>();

		queue.add(() -> pledge.resolve(task.get()));

		try {
			return pledge.get();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Send a task to the given world thread,
	 * but don't wait for it to finish
	 *
	 * @since    0.1.0
	 */
	public static void sendToWorldThread(World world, Runnable task) {

		if (onWorldThread(world)) {
			task.run();
			return;
		}

		Thread wanted_thread = world.bb$getMainThread();
		var queue = THREAD_QUEUES.computeIfAbsent(wanted_thread, t -> new ConcurrentLinkedQueue<>());
		queue.add(task);
	}

	/**
	 * Send a task to the given world thread,
	 * but don't wait for it to finish
	 *
	 * @since    0.1.0
	 */
	public static void sendToWorldThreadWithDebug(World world, Runnable task, String before, String after) {

		Runnable wrapper = () -> {
			BibLog.log("[DEBUG] Starting thread task: " + before);
			try {
				task.run();
			} catch (Throwable e) {
				GlitchGuru.registerThrowable(e, "sendToWorldThreadWithDebug");
			}
			BibLog.log("[DEBUG] Finished thread task: " + after);
		};

		sendToWorldThread(world, wrapper);
	}

	/**
	 * Drain the queue of the current thread
	 *
	 * @since    0.1.0
	 */
	public static void drainOurLocalQueue() {
		drainThreadQueue(Thread.currentThread());
	}

	/**
	 * Drain the queue of the current thread
	 *
	 * @since    0.1.0
	 */
	public static void drainThreadQueue(Thread thread) {
		var queue = THREAD_QUEUES.get(thread);

		if (queue == null) {
			return;
		}

		while (!queue.isEmpty()) {
			Runnable runnable = queue.poll();
			runnable.run();
		}
	}
}