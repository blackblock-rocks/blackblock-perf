package rocks.blackblock.perf.mixin.entity.activation_range.inactive_ticks;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Make sure arrows in the ground keep
 * aging even when they're inactive.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ArrowEntity.class)
public abstract class ArrowEntityMixin extends PersistentProjectileEntity {

    protected ArrowEntityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
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
