package rocks.blackblock.perf.interfaces.chunk_ticking;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.world.ChunkHolder;

/**
 * Let something return chunks that can be ticked
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface TickableChunkSource {
    Long2ObjectLinkedOpenHashMap<ChunkHolder> bb$tickableChunkMap();

    default Iterable<ChunkHolder> bb$tickableChunksIterator() {
        return this.bb$tickableChunkMap().values();
    }
}
