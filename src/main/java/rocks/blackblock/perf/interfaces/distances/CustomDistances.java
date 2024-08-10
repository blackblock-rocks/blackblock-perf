package rocks.blackblock.perf.interfaces.distances;

/**
 * Set custom distances on something
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface CustomDistances {
    void bb$setViewDistance(int view_distance);
    int bb$getViewDistance();

    void bb$setSimulationDistance(int simulation_distance);
    int bb$getSimulationDistance();
}
