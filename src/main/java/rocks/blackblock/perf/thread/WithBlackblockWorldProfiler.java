package rocks.blackblock.perf.thread;

import rocks.blackblock.perf.debug.BlackblockWorldProfiler;

/**
 * Let something have a world profiler
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.2.0
 */
public interface WithBlackblockWorldProfiler {
    default BlackblockWorldProfiler bb$getWorldProfiler() {
        return null;
    }
}
