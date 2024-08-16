package rocks.blackblock.perf.mixin.statistics;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.bib.util.BibPerf;

/**
 * Increment the player stats per second instead of per tick
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixinForStats extends LivingEntity {

    @Shadow
    public abstract void increaseStat(Identifier stat, int amount);

    protected PlayerEntityMixinForStats(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;incrementStat(Lnet/minecraft/util/Identifier;)V"
        )
    )
    private void bb$incrementStatPerSecond(PlayerEntity instance, Identifier stat) {
        if (BibPerf.ON_FULL_SECOND) {
            increaseStat(stat, 20);
        }
    }
}
