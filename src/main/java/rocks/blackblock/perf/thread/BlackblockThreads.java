package rocks.blackblock.perf.thread;

import net.minecraft.server.world.ServerWorld;
import rocks.blackblock.perf.BlackblockPerf;

/**
 * The main threading class,
 * also the initializer.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class BlackblockThreads {

	// The starting thread name
	public static final String THREAD_NAME_PREFIX = BlackblockPerf.MOD_ID + "_server_";

	// How many threads to currently use
	public static int THREADS_COUNT = 0;

	// If we're using threads at all
	public static boolean THREADS_ENABLED = false;

	// The current thread pool
	public static ThreadPool THREAD_POOL = null;

	/**
	 * Temporarily swaps the main thread of given objects to the current thread,
	 * executes a task, and then restores the original threads.
	 * @since    0.1.0
	 */
	public static synchronized void swapThreadsAndRun(Runnable task, WithMutableThread... threaded_instances) {
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
	}

	/**
	 * Is the given thread one of ours?
	 * @since    0.1.0
	 */
	public static boolean ownsThread(Thread thread) {
		return thread.getName().startsWith(THREAD_NAME_PREFIX);
	}
}