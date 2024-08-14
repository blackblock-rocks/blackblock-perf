package rocks.blackblock.perf.mixin.chunk_ticking;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.interfaces.chunk_ticking.TickableChunkSource;

/**
 * Make the ServerChunkLoadingManager keep track of tickable chunks,
 * this way we can skip iterating over inactive chunks every tick.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerChunkLoadingManager.class)
public class ServerChunkLoadingManagerMixin implements TickableChunkSource {

    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> currentChunkHolders;

    @Unique
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> bb$tickable_chunks = new Long2ObjectLinkedOpenHashMap<>();

    @Inject(method = "onChunkStatusChange", at = @At("HEAD"))
    private void listenChunkStatusChange(ChunkPos chunk_pos, ChunkLevelType level_type, CallbackInfo ci) {

        long long_pos = chunk_pos.toLong();
        final ChunkHolder chunk_holder = this.currentChunkHolders.get(long_pos);

        if (chunk_holder == null) {
            return;
        }

        if (chunk_holder.getLevelType().isAfter(ChunkLevelType.BLOCK_TICKING)) {
            this.bb$tickable_chunks.put(long_pos, chunk_holder);
        } else {
            this.bb$tickable_chunks.remove(long_pos);
        }
    }

    @Override
    public Long2ObjectLinkedOpenHashMap<ChunkHolder> bb$tickableChunkMap() {
        return this.bb$tickable_chunks;
    }
}
