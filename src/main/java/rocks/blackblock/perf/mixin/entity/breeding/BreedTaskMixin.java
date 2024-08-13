package rocks.blackblock.perf.mixin.entity.breeding;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.entity.ai.brain.task.BreedTask;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocks.blackblock.perf.activation_range.EntityCluster;
import rocks.blackblock.perf.interfaces.activation_range.ClusteredEntity;

/**
 * Prevent animals from breeding too many mobs
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(BreedTask.class)
public class BreedTaskMixin {

    @WrapWithCondition(
            method = "keepRunning(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;J)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/passive/AnimalEntity;breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;)V"
            )
    )
    private boolean bb$onTick(AnimalEntity instance, ServerWorld world, AnimalEntity other) {

        var cluster = ((ClusteredEntity) instance).bb$getCluster();
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
            Box box = instance.getBoundingBox().expand(64);
            allowed = world.getOtherEntities(instance, box).size() < 48;
        }

        if (!allowed) {
            instance.resetLoveTicks();
            other.resetLoveTicks();
            instance.setBreedingAge(6000);
            other.setBreedingAge(6000);
        }

        return allowed;
    }
}
