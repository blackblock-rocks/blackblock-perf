package rocks.blackblock.perf.mixin.chunk_ticking;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.perf.interfaces.chunk_ticking.TickableChunkSource;

/**
 * Only iterate over the tickable chunks
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerChunkManager.class)
public class ServerChunkManagerMixin {

    @Redirect(
        method = "tickChunks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;entryIterator()Ljava/lang/Iterable;"
        )
    )
    private Iterable<ChunkHolder> bb$redirectVisibleChunks(ServerChunkLoadingManager instance) {
        return ((TickableChunkSource) instance).bb$tickableChunksIterator();
    }
}
