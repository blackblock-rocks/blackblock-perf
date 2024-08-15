package rocks.blackblock.perf.interfaces.distances;

/**
 * Let players have individual view distances
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface PlayerSpecificDistance {

    /**
     * Has the player changed their client-side view distance,
     * and do we need to process it?
     * @since    0.1.0
     */
    default boolean bb$hasDirtyClientSideViewDistance() {
        return false;
    }

    /**
     * The current client-side view distance of the player
     * @since    0.1.0
     */
    default int bb$getClientSideViewDistance() {
        return 6;
    }

    /**
     * Have we calculated a new view distance for the player?
     * @since    0.1.0
     */
    default boolean bb$hasDirtyPersonalViewDistance() {
        return false;
    }

    /**
     * The new view distance of the player
     * @since    0.1.0
     */
    default int bb$getPersonalViewDistance() {
        return 6;
    }

    /**
     * Recalculate the personal view distance of the player
     * @since    0.1.0
     */
    default void bb$recalculatePersonalViewDistance() {
        // no-op
    }

    /**
     * Get the view distance of the world the player is in
     * @since    0.1.0
     */
    default int bb$getWorldViewDistance() {
        return 6;
    }
}
