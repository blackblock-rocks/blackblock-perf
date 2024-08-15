package rocks.blackblock.perf.interfaces.player_watching;

public interface TrackingPositionInfo {
    default boolean bb$isTrackingPositionUpdated() {
        return false;
    }

    default void bb$updateTrackingPosition() {
        // no-op
    }
}
