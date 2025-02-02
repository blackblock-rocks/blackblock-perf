package rocks.blackblock.perf.mixin.distance;

import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.bib.util.BibEntity;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.perf.interfaces.distances.PlayerSpecificDistance;

/**
 * Remember the client-side view distance of the player.
 * This might be lower than the server-side view distance.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixinForDistance implements PlayerSpecificDistance {

    @Shadow
    public abstract ServerWorld getServerWorld();

    @Unique
    private final ServerPlayerEntity bb$self = (ServerPlayerEntity) (Object) this;

    @Unique
    private boolean bb$dirty_client_side_view_distance = false;

    @Unique
    private int bb$client_side_view_distance = 6;

    @Unique
    private boolean bb$dirty_personal_view_distance = false;

    @Unique
    private int bb$personal_view_distance = 6;

    @Unique
    private int bb$reduced_view_distance_count = 0;

    @Unique
    private boolean bb$has_reduced_view_distance = false;

    /**
     * Listen for client setting changes
     * @since    0.1.0
     */
    @Inject(method = "setClientOptions", at = @At("HEAD"))
    private void onClientSettingsChanged(SyncedClientOptions packet, CallbackInfo ci) {
        final int new_view_distance = packet.viewDistance();

        if (new_view_distance != this.bb$client_side_view_distance) {
            this.bb$dirty_client_side_view_distance = true;
        }

        // Always set the client view distance to at least 2
        this.bb$client_side_view_distance = Math.max(2, new_view_distance);

        this.bb$recalculatePersonalViewDistance();
    }

    /**
     * Copy the client view distance from the old player
     * @since    0.1.0
     */
    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void onPlayerCopy(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.bb$client_side_view_distance = oldPlayer.bb$getClientSideViewDistance();
        this.bb$dirty_client_side_view_distance = true;

        this.bb$personal_view_distance = oldPlayer.bb$getPersonalViewDistance();
        this.bb$dirty_personal_view_distance = true;
    }

    @Unique
    @Override
    public int bb$getWorldViewDistance() {
        return this.getServerWorld().bb$getMaxViewDistance();
    }

    @Unique
    @Override
    public boolean bb$hasDirtyClientSideViewDistance() {
        return this.bb$dirty_client_side_view_distance;
    }

    @Unique
    @Override
    public int bb$getClientSideViewDistance() {
        this.bb$dirty_client_side_view_distance = false;
        return this.bb$client_side_view_distance;
    }

    @Unique
    @Override
    public boolean bb$hasDirtyPersonalViewDistance() {
        return this.bb$dirty_personal_view_distance;
    }

    @Unique
    @Override
    public int bb$getPersonalViewDistance() {
        this.bb$dirty_personal_view_distance = false;
        return this.bb$personal_view_distance;
    }

    /**
     * Recalculate the personal view distance of the player
     * with the given max view distance
     * @since    0.1.0
     */
    @Unique
    @Override
    public void bb$recalculatePersonalViewDistance() {

        int max_view_distance = this.bb$getWorldViewDistance();
        boolean is_afk = this.bb$self.bb$isAfk();

        if (is_afk) {
            max_view_distance = 5;
        } else if (max_view_distance > 5) {
            ServerWorld world = this.getServerWorld();
            var info = world.bb$getPerformanceInfo();

            if (info.isOverloaded()) {
                if (BibEntity.isInCave(this.bb$self)) {
                    this.bb$reduced_view_distance_count++;
                } else if (BibEntity.isInEnclosedSpace(this.bb$self)) {
                    this.bb$reduced_view_distance_count++;
                } else {
                    this.bb$reduced_view_distance_count--;
                }

                if (this.bb$reduced_view_distance_count > 5) {
                    this.bb$reduced_view_distance_count = 5;
                } else if (this.bb$reduced_view_distance_count < 0) {
                    this.bb$reduced_view_distance_count = 0;
                }

                if (this.bb$reduced_view_distance_count > 3) {
                    this.bb$has_reduced_view_distance = true;
                    max_view_distance = 5;
                } else if (this.bb$reduced_view_distance_count == 0) {
                    this.bb$has_reduced_view_distance = false;
                } else if (this.bb$has_reduced_view_distance) {
                    max_view_distance = 5;
                }
            } else {
                this.bb$reduced_view_distance_count = 0;
                this.bb$has_reduced_view_distance = false;
            }
        }

        int new_personal_distance = Math.min(this.bb$client_side_view_distance, max_view_distance);

        if (new_personal_distance != this.bb$personal_view_distance) {
            this.bb$dirty_personal_view_distance = true;
            this.bb$personal_view_distance = new_personal_distance;
        }
    }
}
