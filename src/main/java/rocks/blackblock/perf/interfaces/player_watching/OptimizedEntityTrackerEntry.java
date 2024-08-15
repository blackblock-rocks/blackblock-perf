package rocks.blackblock.perf.interfaces.player_watching;

public interface OptimizedEntityTrackerEntry {
    default void bb$tickAlways() {
        // no-op
    }

    default void bb$syncEntityData() {
        // no-op
    }
}
