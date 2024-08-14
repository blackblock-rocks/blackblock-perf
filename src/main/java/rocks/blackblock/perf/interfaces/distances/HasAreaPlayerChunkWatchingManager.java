package rocks.blackblock.perf.interfaces.distances;

import rocks.blackblock.perf.distance.AreaPlayerChunkWatchingManager;

/**
 * Does this class have an AreaPlayerChunkWatchingManager?
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface HasAreaPlayerChunkWatchingManager {
    default AreaPlayerChunkWatchingManager bb$getAreaPlayerChunkWatchingManager() {
        return null;
    }
}
