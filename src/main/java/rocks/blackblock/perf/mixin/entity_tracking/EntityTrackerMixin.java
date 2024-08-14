package rocks.blackblock.perf.mixin.entity_tracking;

import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Cache the max track distance of an entity tracker.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerChunkLoadingManager.EntityTracker.class)
public abstract class EntityTrackerMixin {

    // This refers to the outer ServerChunkLoadingManager instance
    @Shadow
    @Final
    ServerChunkLoadingManager field_18245;

    @Shadow
    protected abstract int getMaxTrackDistance();

    @Unique
    private int last_distance_update = 0;

    @Unique
    private int cached_max_distance = 0;

    @Redirect(
        method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerChunkLoadingManager$EntityTracker;getMaxTrackDistance()I"
        )
    )
    private int bb$redirectGetMaxTrackDistance(ServerChunkLoadingManager.EntityTracker instance) {

        final int ticks = this.field_18245.world.getServer().getTicks();

        if (this.last_distance_update != ticks || this.cached_max_distance == 0) {
            final int max_track_distance = this.getMaxTrackDistance();
            this.cached_max_distance = max_track_distance;
            this.last_distance_update = ticks;
            return max_track_distance;
        }

        return this.cached_max_distance;
    }
}
