package rocks.blackblock.perf.mixin.entity.activation_range;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.activation_range.DynamicActivationRange;
import rocks.blackblock.perf.activation_range.EntityCluster;
import rocks.blackblock.perf.activation_range.EntityClusterManager;
import rocks.blackblock.perf.interfaces.activation_range.HasEntityClusters;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Actually do all the inactivity logic
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements HasEntityClusters {

    @Unique
    private final ServerWorld bb$self = (ServerWorld) (Object) this;

    @Shadow
    @Final
    private MinecraftServer server;

    @Unique
    private EntityClusterManager entity_cluster_manager = new EntityClusterManager(bb$self);

    /**
     * Check all entities in this world
     * @since 0.1.0
     */
    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/EntityList;forEach(Ljava/util/function/Consumer;)V"
            )
    )
    private void bb$optimizeEntityActivation(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        final int current_tick = this.server.getTicks();

        if (current_tick % 20 == 0) {

            // Recreate the entity groups every 4 seconds
            if (current_tick % 80 == 0) {
                this.entity_cluster_manager.recreateEntityGroups();
            }

            DynamicActivationRange.triggerActivation(this.bb$self, current_tick);
        }
    }

    /**
     * Get the entity groups
     * @since 0.1.0
     */
    @Override
    @Unique
    public List<EntityCluster> bb$getEntityClusters() {
        return this.entity_cluster_manager.bb$getEntityClusters();
    }

    /**
     * Handle ticking a non-passenger entity in this world
     * @since 0.1.0
     */
    @WrapWithCondition(
            method = "tickEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;tick()V"
            )
    )
    private boolean bb$shouldTickEntity(Entity entity) {
        if (DynamicActivationRange.checkIfActive(entity, this.server.getTicks())) {
            entity.bb$setInactive(false);
            entity.age++;
            return true;
        } else {
            entity.bb$setInactive(true);
            entity.bb$inactiveTick();
            return false;
        }
    }

    /**
     * Handle ticking a passenger entity in this world
     * @since 0.1.0
     */
    @WrapWithCondition(
            method = "tickPassenger",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;tickRiding()V"
            )
    )
    private boolean bb$shouldTickPassenger(Entity passenger, Entity vehicle, Entity ignored) {
        if (DynamicActivationRange.checkIfActive(passenger, this.server.getTicks())) {
            passenger.bb$setInactive(false);
            passenger.age++;
            return true;
        } else {
            passenger.setVelocity(Vec3d.ZERO);
            passenger.bb$setInactive(true);
            passenger.bb$inactiveTick();
            vehicle.updatePassengerPosition(passenger);
            return false;
        }
    }

    /**
     * Do not increment the tick/age count here,
     * we'll let our custom logic do that later.
     * @since 0.1.0
     */
    @Redirect(
            method = {"tickEntity", "tickPassenger"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/Entity;age:I",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void bb$redirectTickCount(Entity entity, int value) {
        entity.bb$incrementPotentialTickCount();
    }
}
