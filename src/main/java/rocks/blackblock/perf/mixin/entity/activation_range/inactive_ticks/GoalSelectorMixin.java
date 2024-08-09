package rocks.blackblock.perf.mixin.entity.activation_range.inactive_ticks;

import net.minecraft.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import rocks.blackblock.perf.interfaces.activation_range.InactiveTickable;

/**
 * Make the GoalSelector tick at least once every 20 ticks,
 * even when it's inactive
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin implements InactiveTickable {

    @Shadow public abstract void tick();

    @Unique
    private int bb$current_rate;

    @Override
    public void bb$inactiveTick() {
        if (++this.bb$current_rate % 20 == 0) {
            this.tick();
        }
    }
}
