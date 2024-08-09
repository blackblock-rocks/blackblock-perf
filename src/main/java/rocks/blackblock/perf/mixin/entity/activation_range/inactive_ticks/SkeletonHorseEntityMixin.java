package rocks.blackblock.perf.mixin.entity.activation_range.inactive_ticks;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Make the Skeleton Horse despawn after the trap has ended,
 * even if it's inactive.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(SkeletonHorseEntity.class)
public abstract class SkeletonHorseEntityMixin extends AbstractHorseEntity {

    @Shadow
    private boolean trapped;

    @Shadow
    private int trapTime;

    @Shadow
    @Final
    private static int DESPAWN_AGE;

    protected SkeletonHorseEntityMixin(EntityType<? extends AbstractHorseEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void bb$inactiveTick() {
        super.bb$inactiveTick();

        if (this.trapped && this.trapTime++ >= DESPAWN_AGE) {
            this.discard();
        }
    }
}
