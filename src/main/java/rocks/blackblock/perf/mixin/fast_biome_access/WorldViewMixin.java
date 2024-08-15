package rocks.blackblock.perf.mixin.fast_biome_access;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;

@Mixin(WorldView.class)
public interface WorldViewMixin {

    @Redirect(method = "getBiomeForNoiseGen", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldView;getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;"))
    private Chunk redirectBiomeChunk(WorldView instance, int x, int z, ChunkStatus chunkStatus, boolean create) {
        if (!create && instance instanceof ServerWorld world) {
            final ChunkHolder holder = world.getChunkManager().chunkLoadingManager.getChunkHolder(ChunkPos.toLong(x, z));
            if (holder != null) {
                final CompletableFuture<OptionalChunk<WorldChunk>> future = holder.getAccessibleFuture();
                final OptionalChunk<WorldChunk> either = future.getNow(null);
                if (either != null) {
                    final WorldChunk chunk = either.orElse(null);
                    if (chunk != null) return chunk;
                }
            }
        }
        return instance.getChunk(x, z, chunkStatus, create);
    }
}
