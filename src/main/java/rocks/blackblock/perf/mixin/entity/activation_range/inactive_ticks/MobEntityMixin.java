package rocks.blackblock.perf.mixin.entity.activation_range.inactive_ticks;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * This is where we actually make the {@link GoalSelector}
 * perform its inactive ticks.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {

    @Shadow @Final protected GoalSelector goalSelector;

    @Shadow @Final protected GoalSelector targetSelector;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void bb$inactiveTick() {
        super.bb$inactiveTick();
        this.goalSelector.bb$inactiveTick();
        this.targetSelector.bb$inactiveTick();
    }
}
