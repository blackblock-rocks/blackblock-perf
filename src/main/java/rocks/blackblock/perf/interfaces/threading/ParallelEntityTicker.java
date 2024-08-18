package rocks.blackblock.perf.interfaces.threading;

import rocks.blackblock.perf.thread.ThreadPool;

public interface ParallelEntityTicker {
    default ThreadPool bb$getEntityThreadPool() {
        return null;
    }
}
