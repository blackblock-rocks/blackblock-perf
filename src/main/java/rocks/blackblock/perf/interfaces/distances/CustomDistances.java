package rocks.blackblock.perf.interfaces.distances;

/**
 * Set custom distances on something
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface CustomDistances {
    default void bb$setMaxViewDistance(int view_distance) {
        // no-op
    }

    default int bb$getMaxViewDistance() {
        return 9;
    }

    default void bb$setSimulationDistance(int simulation_distance) {
        // no-op
    }

    default int bb$getSimulationDistance() {
        return 9;
    }
}
