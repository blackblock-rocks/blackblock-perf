package rocks.blackblock.perf.mixin.entity.activation_range.inactive_ticks;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Make sure AreaEffectCloudEntities get
 * discarded on time when they're inactive.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(AreaEffectCloudEntity.class)
public abstract class AreaEffectCloudEntityMixin extends Entity {

    @Shadow private int waitTime;

    @Shadow private int duration;

    public AreaEffectCloudEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void bb$inactiveTick() {
        super.bb$inactiveTick();

        if (++this.age >= this.waitTime + this.duration) {
            this.discard();
        }
    }
}
