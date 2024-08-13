package rocks.blackblock.perf.mixin.sync_load;

import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.bib.util.BibChunk;

/**
 * Don't load chunks for dynamic game events.
 * Doing so can cause the server to freeze indefinitely.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(EntityGameEventHandler.class)
public class EntityGameEventHandlerMixin {

    @Redirect(
            method = "updateDispatcher",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldView;getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;"
            )
    )
    private static Chunk bb$onlyUpdateIfLoaded(WorldView level, int x, int z, ChunkStatus status, boolean bl) {
        return BibChunk.getChunkNow(level, x, z);
    }
}
