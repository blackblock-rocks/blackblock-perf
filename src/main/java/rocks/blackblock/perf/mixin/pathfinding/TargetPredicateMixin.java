package rocks.blackblock.perf.mixin.pathfinding;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Do an early max distance check when finding a target
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(TargetPredicate.class)
public class TargetPredicateMixin {

    @Shadow
    private double baseMaxDistance;

    @Inject(
        method = "test",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;getAttackDistanceScalingFactor(Lnet/minecraft/entity/Entity;)D",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void quickCancelPathFinding(ServerWorld world, LivingEntity baseEntity, LivingEntity targetEntity, CallbackInfoReturnable<Boolean> cir, @Share("dist") LocalDoubleRef doubleRef) {
        double f = baseEntity.squaredDistanceTo(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ());
        doubleRef.set(f);
        if (f > this.baseMaxDistance * this.baseMaxDistance) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
        method = "test",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;squaredDistanceTo(DDD)D"
        )
    )
    private double borrowValueFromOtherMixin(LivingEntity instance, double x, double y, double z, @Share("dist")LocalDoubleRef doubleRef) {
        return doubleRef.get();
    }
}
