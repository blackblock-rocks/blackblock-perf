package rocks.blackblock.perf.interfaces.chunk_ticking;

public interface ResettableIceAndSnowTicks {
    default void bb$resetIceAndSnowTick() {
        // no-op
    }
}
