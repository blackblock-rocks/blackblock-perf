package rocks.blackblock.perf.mixin.entity.activation_range.inactive_ticks;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Make sure spectral arrows in the ground keep
 * aging even when they're inactive.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(SpectralArrowEntity.class)
public abstract class SpectralArrowEntityMixin extends PersistentProjectileEntity {

    protected SpectralArrowEntityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void bb$inactiveTick() {
        super.bb$inactiveTick();

        if (this.isInGround()) {
            this.age();
        }
    }
}
