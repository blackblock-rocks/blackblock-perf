package rocks.blackblock.perf.mixin.sync_load;

import net.minecraft.entity.ai.goal.StepAndDestroyBlockGoal;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.bib.util.BibChunk;

/**
 * Don't load chunks in order to find a block to destroy
 * (Used by zombies for finding turtle eggs)
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(StepAndDestroyBlockGoal.class)
public class StepAndDestroyBlockGoalMixin {

    @Redirect(
        method = "isTargetPos",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/WorldView;getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;"
        )
    )
    private Chunk bb$onlyValidateIfLoaded(WorldView level, int x, int z, ChunkStatus status, boolean create) {
        return BibChunk.getChunkNow(level, x, z);
    }
}
