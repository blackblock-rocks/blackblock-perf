package rocks.blackblock.perf.mixin.entity.breeding;

import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.activation_range.EntityCluster;
import rocks.blackblock.perf.interfaces.activation_range.ClusteredEntity;

/**
 * Prevent animals from breeding too many mobs
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(AnimalMateGoal.class)
public class AnimalMateGoalMixin {

    @Shadow @Final protected AnimalEntity animal;

    @Shadow @Nullable protected AnimalEntity mate;

    @Inject(method = "breed", at = @At("HEAD"), cancellable = true)
    private void bb$onBreed(CallbackInfo ci) {

        var cluster = ((ClusteredEntity) this.animal).bb$getCluster();
        boolean allowed = true;

        if (cluster != null) {
            if (cluster.getSize() > 28) {
                allowed = false;
            } else {
                EntityCluster super_cluster = cluster.getSuperCluster();
                if (super_cluster != null) {
                    if (super_cluster.getSize() > 32) {
                        allowed = false;
                    }
                }
            }
        }

        if (allowed) {
            Box box = this.animal.getBoundingBox().expand(64);
            allowed = this.animal.getWorld().getOtherEntities(this.animal, box).size() < 48;
        }

        if (!allowed) {
            this.animal.resetLoveTicks();
            this.animal.setBreedingAge(6000);
            this.mate.resetLoveTicks();
            this.mate.setBreedingAge(6000);
            ci.cancel();
        }
    }

}
