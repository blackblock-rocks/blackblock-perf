package rocks.blackblock.perf.mixin.player_watching.optimize_nearby_entity_tracking_lookups;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.bib.monitor.GlitchGuru;
import rocks.blackblock.perf.distance.NearbyEntityTracking;

import java.util.List;

@Mixin(ServerChunkLoadingManager.class)
public class ServerChunkLoadingManagerMixinForEntityTracking {

    @Shadow
    @Final
    private Int2ObjectMap<ServerChunkLoadingManager.EntityTracker> entityTrackers;

    @Shadow
    @Final
    private ServerChunkLoadingManager.TicketManager ticketManager;

    @Unique
    private final NearbyEntityTracking nearbyEntityTracking = new NearbyEntityTracking();


    @Redirect(
        method = "loadEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerChunkLoadingManager$EntityTracker;updateTrackedStatus(Ljava/util/List;)V"
        )
    )
    private void bb$redirectUpdateOnAddEntity(ServerChunkLoadingManager.EntityTracker instance, List<ServerPlayerEntity> players) {
        this.nearbyEntityTracking.addEntityTracker(instance);
    }

    @Redirect(
        method = "loadEntity",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;"
        )
    )
    private <T> ObjectCollection<T> bb$nullifyTrackerListOnAddEntity(Int2ObjectMap<T> instance) {
        if (this.entityTrackers == instance) return Int2ObjectMaps.<T>emptyMap().values();
        else return instance.values();
    }

    @Redirect(
        method = "unloadEntity",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;"
        )
    )
    private <T> ObjectCollection<T> bb$nullifyTrackerListOnRemoveEntity(Int2ObjectMap<T> instance) {
        if (this.entityTrackers == instance) return Int2ObjectMaps.<T>emptyMap().values();
        else return instance.values();
    }

    @Redirect(
        method = "unloadEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerChunkLoadingManager$EntityTracker;stopTracking()V"
        )
    )
    private void bb$redirectUpdateOnRemoveEntity(ServerChunkLoadingManager.EntityTracker instance) {
        this.nearbyEntityTracking.removeEntityTracker(instance);
        instance.stopTracking();
    }

    @Redirect(
        method = "updatePosition",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;"
        )
    )
    private <T> ObjectCollection<T> bb$redirectTrackersOnUpdatePosition(Int2ObjectMap<T> instance, ServerPlayerEntity player) {
        if (this.entityTrackers != instance) {
            return instance.values();
        } else {
            return Int2ObjectMaps.<T>emptyMap().values(); // nullify, already handled in tick call
        }
    }

    /**
     * @author ishland
     * @reason use nearby tracker lookup
     */
    @Overwrite
    public void tickEntityMovement() {
        try {
            this.nearbyEntityTracking.tick(this.ticketManager);
        } catch (Throwable t) {
            GlitchGuru.registerThrowable(t, "ServerChunkLoadingManager tickEntityMovement");
        }
    }
}
