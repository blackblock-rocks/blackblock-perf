package rocks.blackblock.perf.mixin.entity.activation_range.inactive_ticks;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.TadpoleEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Let tadpoles keep aging even when they're inactive.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(TadpoleEntity.class)
public abstract class TadpoleEntityMixin extends FishEntity {

    @Shadow protected abstract void setTadpoleAge(int tadpoleAge);

    public TadpoleEntityMixin(EntityType<? extends FishEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void bb$inactiveTick() {
        super.bb$inactiveTick();
        this.setTadpoleAge(this.age + 1);
    }
}
