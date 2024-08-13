package rocks.blackblock.perf.mixin.sync_load;

import net.minecraft.item.FilledMapItem;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.bib.util.BibChunk;

/**
 * Prevent filled maps from loading chunks
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(FilledMapItem.class)
public class FilledMapItemMixin {

    @Redirect(
            method = "updateColors",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getChunk(II)Lnet/minecraft/world/chunk/WorldChunk;"
            )
    )
    private WorldChunk bb$onlyUpdateIfLoaded(World level, int chunkX, int chunkZ) {
        return (WorldChunk) BibChunk.getChunkNow(level, chunkX, chunkZ);
    }

    @Redirect(
            method = "updateColors",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/WorldChunk;isEmpty()Z"
            )
    )
    private boolean bb$validateNotNull(WorldChunk chunk) {
        return chunk == null || chunk.isEmpty();
    }
}
