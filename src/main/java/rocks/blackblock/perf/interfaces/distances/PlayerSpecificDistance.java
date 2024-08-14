package rocks.blackblock.perf.interfaces.distances;

/**
 * Let players have individual view distances
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface PlayerSpecificDistance {
    default boolean bb$hasDirtyClientViewDistance() {
        return false;
    }

    default int bb$getClientViewDistance() {
        return 2;
    }
}
