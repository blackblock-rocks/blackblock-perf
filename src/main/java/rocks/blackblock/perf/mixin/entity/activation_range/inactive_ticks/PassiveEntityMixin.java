package rocks.blackblock.perf.mixin.entity.activation_range.inactive_ticks;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Let Passive Entities' age value tick
 * even when they're inactive.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(PassiveEntity.class)
public abstract class PassiveEntityMixin extends PathAwareEntity {

    @Shadow public abstract int getBreedingAge();

    @Shadow public abstract void setBreedingAge(int age);

    protected PassiveEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void bb$inactiveTick() {
        super.bb$inactiveTick();

        final int age = this.getBreedingAge();
        if (age < 0) {
            this.setBreedingAge(age + 1);
        } else if (age > 0) {
            this.setBreedingAge(age - 1);
        }
    }
}
