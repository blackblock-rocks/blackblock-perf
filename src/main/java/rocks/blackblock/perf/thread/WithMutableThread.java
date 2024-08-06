package rocks.blackblock.perf.thread;

/**
 * Makes an instance have mutable thread
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface WithMutableThread {
    Thread bb$getMainThread();
    void bb$setMainThread(Thread thread);
}
