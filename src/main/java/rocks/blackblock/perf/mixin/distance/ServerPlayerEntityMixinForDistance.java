package rocks.blackblock.perf.mixin.distance;

import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.interfaces.distances.PlayerSpecificDistance;

/**
 * Remember the client-side view distance of the player.
 * This might be lower than the server-side view distance.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixinForDistance implements PlayerSpecificDistance {

    @Unique
    private boolean view_distance_dirty = false;

    @Unique
    private int client_view_distance = 2;

    /**
     * Listen for client setting changes
     * @since    0.1.0
     */
    @Inject(method = "setClientOptions", at = @At("HEAD"))
    private void onClientSettingsChanged(SyncedClientOptions packet, CallbackInfo ci) {
        final int new_view_distance = packet.viewDistance();

        if (new_view_distance != this.client_view_distance) {
            this.view_distance_dirty = true;
        }

        // Always set the client view distance to at least 2
        this.client_view_distance = Math.max(2, this.client_view_distance);
    }

    /**
     * Copy the client view distance from the old player
     * @since    0.1.0
     */
    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void onPlayerCopy(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.client_view_distance = oldPlayer.bb$getClientViewDistance();
        this.view_distance_dirty = true;
    }

    @Override
    public boolean bb$hasDirtyClientViewDistance() {
        return this.view_distance_dirty;
    }

    @Override
    public int bb$getClientViewDistance() {
        this.view_distance_dirty = false;
        return this.client_view_distance;
    }
}
