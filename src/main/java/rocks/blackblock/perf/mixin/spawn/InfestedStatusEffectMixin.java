package rocks.blackblock.perf.mixin.spawn;

import net.minecraft.entity.effect.InfestedStatusEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import rocks.blackblock.perf.spawn.DynamicSpawns;

/**
 * Prevent the Infested effect from spawning too many mobs
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(InfestedStatusEffect.class)
public class InfestedStatusEffectMixin {

    @Inject(
            method = "spawnSilverfish",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getRandom()Lnet/minecraft/util/math/random/Random;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void bb$enforceMobcap(World world, LivingEntity entity, double x, double y, double z, CallbackInfo ci, SilverfishEntity silverfishEntity) {
        boolean can_spawn = DynamicSpawns.canSpawnForFarm(
                silverfishEntity,
                entity.getWorld(),
                entity.getBlockPos()
        );

        if (!can_spawn) {
            ci.cancel();
        }
    }
}
