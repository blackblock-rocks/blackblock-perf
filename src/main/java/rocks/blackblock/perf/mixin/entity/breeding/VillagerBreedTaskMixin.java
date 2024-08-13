package rocks.blackblock.perf.mixin.entity.breeding;

import net.minecraft.entity.ai.brain.task.VillagerBreedTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.activation_range.EntityCluster;
import rocks.blackblock.perf.interfaces.activation_range.ClusteredEntity;

/**
 * Prevent villagers from breeding too many mobs
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(VillagerBreedTask.class)
public class VillagerBreedTaskMixin {

    @Inject(
        method = "keepRunning(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/VillagerEntity;J)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/VillagerEntity;eatForBreeding()V",
            ordinal = 0
        ),
        cancellable = true
    )
    private void bb$onTick(ServerWorld serverWorld, VillagerEntity villagerEntity, long l, CallbackInfo ci) {

        var cluster = ((ClusteredEntity) villagerEntity).bb$getCluster();
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
            Box box = villagerEntity.getBoundingBox().expand(64);
            allowed = serverWorld.getOtherEntities(villagerEntity, box, entity -> entity instanceof VillagerEntity).size() < 48;
        }

        if (!allowed) {
            ci.cancel();
        }
    }
}
