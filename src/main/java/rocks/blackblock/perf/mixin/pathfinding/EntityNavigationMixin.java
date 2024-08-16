package rocks.blackblock.perf.mixin.pathfinding;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Don't recalculate paths when the server is overloaded.
 * Also don't tick if the entity is in a vehicle.
 * And prevent pathfinding from being stuck in a loop (PaperMC patch #0371)
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(EntityNavigation.class)
public abstract class EntityNavigationMixin {

    @Shadow
    @Nullable
    protected Path currentPath;

    @Shadow
    public abstract boolean startMovingAlong(@Nullable Path path, double speed);

    @Shadow
    protected boolean inRecalculationCooldown;

    @Shadow
    @Final
    protected World world;

    @Shadow
    @Final
    protected MobEntity entity;

    @Unique
    private int bb$last_movement_failure;

    @Unique
    private int bb$pathfind_failures;

    @Inject(
        method = "startMovingTo(Lnet/minecraft/entity/Entity;D)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void startMovingTo(Entity entity, double speed, CallbackInfoReturnable<Boolean> cir) {
        if (this.bb$pathfind_failures > 10 && this.currentPath == null && entity.getServer().getTicks() < this.bb$last_movement_failure + 40) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
        method = "startMovingTo(Lnet/minecraft/entity/Entity;D)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/pathing/EntityNavigation;startMovingAlong(Lnet/minecraft/entity/ai/pathing/Path;D)Z"
        )
    )
    private boolean insertCancellation(EntityNavigation instance, Path path, double speed) {
        if (this.startMovingAlong(path, speed)) {
            this.bb$last_movement_failure = 0;
            this.bb$pathfind_failures = 0;
            return true;
        } else {
            this.bb$pathfind_failures++;
            this.bb$last_movement_failure = this.entity.getServer().getTicks();
            return false;
        }
    }

    /**
     * Just don't tick if the entity is in a vehicle
     *
     * @since    0.1.0
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void bb$onTick(CallbackInfo ci) {
        if (entity.hasVehicle()) {
            ci.cancel();
        }
    }

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
