package rocks.blackblock.perf.mixin.pathfinding;

import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.bib.util.BibLog;

/**
 * Don't recalculate paths when the server is overloaded
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(EntityNavigation.class)
public class EntityNavigationMixin {

    @Shadow
    protected boolean inRecalculationCooldown;

    @Shadow
    @Final
    protected World world;

    @Shadow @Final protected MobEntity entity;

    /**
     * The ServerWorld class checks every EntityNavigation instance
     * to see if it should recalculate its path.
     *
     * It if returns true, it will be added to a list,
     * and the {@link EntityNavigation#recalculatePath} method will be called.
     *
     * @since    0.1.0
     */
    @Inject(
        method = "shouldRecalculatePath",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void bb$OnShouldRecalculatePath(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {

        if (this.inRecalculationCooldown) {
            return;
        }

        if (this.world.bb$getPerformanceInfo().isRandomlyDisabled()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * This method can be called weather or not `shouldRecalculatePath` returned true.
     *
     * So we will cancel the call if the server is busy.
     *
     * @since    0.1.0
     */
    @Inject(
            method = "recalculatePath",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void bb$onRecalculatePath(CallbackInfo ci) {
        if (this.world.bb$getPerformanceInfo().isRandomlyDisabled()) {
            ci.cancel();
            return;
        }

        var cluster = this.entity.bb$getCluster();

        // If the entity is in a large cluster
        if (cluster != null && cluster.getScore() > 5 && cluster.getSize() > 15) {
            ci.cancel();
        }
    }
}
