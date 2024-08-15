package rocks.blackblock.perf.mixin.player_watching.optimize_nearby_entity_tracking_lookups;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.PlayerAssociatedNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.interfaces.player_watching.OptimizedEntityTracker;

import java.util.List;
import java.util.Set;

@Mixin(ServerChunkLoadingManager.EntityTracker.class)
public abstract class EntityTrackerMixin implements OptimizedEntityTracker {

    @Shadow
    @Final
    public Entity entity;

    @Shadow
    @Final
    private Set<PlayerAssociatedNetworkHandler> listeners;

    @Shadow
    @Final
    EntityTrackerEntry entry;

    @Shadow
    public ChunkSectionPos trackedSection;

    @Unique
    private double bb$prev_x = Double.NaN;

    @Unique
    private double bb$prev_y = Double.NaN;

    @Unique
    private double bb$prev_z = Double.NaN;

    @Override
    public boolean bb$isPositionUpdated() {
        final Vec3d pos = this.entity.getPos();
        return pos.x != this.bb$prev_x || pos.y != this.bb$prev_y || pos.z != bb$prev_z;
    }

    @Override
    public void bb$updatePosition() {
        final Vec3d pos = this.entity.getPos();
        this.bb$prev_x = pos.x;
        this.bb$prev_y = pos.y;
        this.bb$prev_z = pos.z;
    }

    @Override
    public void bb$tryTick() {
        this.trackedSection = ChunkSectionPos.from(this.entity);
        if (!this.listeners.isEmpty()) {
            this.entry.tick();
        } else {
            final List<Entity> current_passengers = this.entity.getPassengerList();

            if (!this.entry.lastPassengers.equals(current_passengers)) {
                this.entry.lastPassengers = current_passengers;
            }

            if (this.entity instanceof ServerPlayerEntity player) {
                // for some reasons mojang decides to sync entity data here, so we need to do it manually

                if (this.entity.velocityModified) {
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(this.entity));
                    this.entity.velocityModified = false;
                }

                this.entry.bb$syncEntityData();
            }
        }
    }

    @Inject(
        method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Set;add(Ljava/lang/Object;)Z",
            shift = At.Shift.BEFORE
        )
    )
    private void bb$beforeStartTracking(ServerPlayerEntity player, CallbackInfo ci) {
        if (this.listeners.isEmpty()) {
            this.entry.bb$tickAlways();
        }
    }

    @Redirect(
        method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;isTracked(Lnet/minecraft/server/network/ServerPlayerEntity;II)Z"
        )
    )
    private boolean bb$assumeAlwaysTracked(ServerChunkLoadingManager instance, ServerPlayerEntity player, int chunkX, int chunkZ) {
        return true;
    }
}
