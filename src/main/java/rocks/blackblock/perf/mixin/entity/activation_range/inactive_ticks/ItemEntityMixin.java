package rocks.blackblock.perf.mixin.entity.activation_range.inactive_ticks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Make dropped items keep track of their age
 * even when they're inactive
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    @Shadow
    private int pickupDelay;

    @Shadow
    @Final
    private static int CANNOT_PICK_UP_DELAY;

    @Shadow
    @Final
    private static int NEVER_DESPAWN_AGE;

    @Shadow
    @Final
    private static int DESPAWN_AGE;

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void bb$inactiveTick() {
        super.bb$inactiveTick();

        if (this.pickupDelay > 0 && this.pickupDelay != CANNOT_PICK_UP_DELAY) {
            this.pickupDelay--;
        }

        if (this.age != NEVER_DESPAWN_AGE) {
            this.age++;
        }

        if (this.age >= DESPAWN_AGE) {
            this.discard();
        }
    }
}
