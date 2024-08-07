package rocks.blackblock.perf.mixin.spawn;

import net.minecraft.entity.effect.InfestedStatusEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
            method = "onEntityDamage",
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/ToIntFunction;applyAsInt(Ljava/lang/Object;)I",
                    ordinal = 0
            )
    )
    private void bb$enforceMobcap(LivingEntity entity, int amplifier, DamageSource source, float damage, CallbackInfo ci) {
        boolean can_spawn = DynamicSpawns.canSpawn(
                entity,
                entity.getWorld(),
                entity.getBlockPos()
        );

        if (!can_spawn) {
            ci.cancel();
        }
    }
}
