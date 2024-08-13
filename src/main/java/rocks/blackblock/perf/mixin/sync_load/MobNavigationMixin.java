package rocks.blackblock.perf.mixin.sync_load;

import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.bib.util.BibChunk;

/**
 * Don't load chunks for pathfinding.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(MobNavigation.class)
public abstract class MobNavigationMixin extends EntityNavigation {

    public MobNavigationMixin(MobEntity entity, World world) {
        super(entity, world);
    }

    @Inject(method = "findPathTo(Lnet/minecraft/util/math/BlockPos;I)Lnet/minecraft/entity/ai/pathing/Path;", at = @At("HEAD"), cancellable = true)
    private void bb$onlyPathfindIfLoaded(BlockPos target, int distance, CallbackInfoReturnable<Path> cir) {
        if (!BibChunk.isChunkLoaded(this.world, target)) {
            cir.setReturnValue(null);
        }
    }
}
