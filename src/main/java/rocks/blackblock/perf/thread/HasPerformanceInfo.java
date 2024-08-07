package rocks.blackblock.perf.thread;

import rocks.blackblock.bib.util.BibPerf;

/**
 * Get the performance info from a world
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface HasPerformanceInfo {
    BibPerf.Info bb$getPerformanceInfo();
}
