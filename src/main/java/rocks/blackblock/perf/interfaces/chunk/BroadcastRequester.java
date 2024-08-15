package rocks.blackblock.perf.interfaces.chunk;

import net.minecraft.server.world.ChunkHolder;

public interface BroadcastRequester {
    default void bb$requiresBroadcast(ChunkHolder holder) {
        // no-op
    }
}
