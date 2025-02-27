package rocks.blackblock.perf.mixin.entity.suffocation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.bib.util.BibPerf;

/**
 * Only check for suffocation damage every half second
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixinForSuffocation extends Entity {

    public LivingEntityMixinForSuffocation(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract boolean isInsideWall();

    @Shadow
    protected float lastDamageTaken;

    @Shadow
    @Final
    public int defaultMaxHealth;

    @Redirect(
        method = "baseTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;isInsideWall()Z"
        )
    )
    private boolean redirectWallChecks(LivingEntity instance) {
        return ((BibPerf.ON_HALF_SECOND && bb$couldPossiblyBeHurt(1.0F))) && this.isInsideWall();
    }

    @Unique
    public boolean bb$couldPossiblyBeHurt(float amount) {
        return !((float) this.timeUntilRegen > (float) this.defaultMaxHealth / 2.0F) || !(amount <= this.lastDamageTaken);
    }
}
