package rocks.blackblock.perf.interfaces.player_watching;

public interface OptimizedEntityTracker {

    default boolean bb$isPositionUpdated() {
        return false;
    }

    default void bb$updatePosition() {
        // no-op
    }

    default void bb$tryTick() {
        // no-op
    }
}
