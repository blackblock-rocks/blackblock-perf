package rocks.blackblock.perf.mixin.distance;

import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.network.packet.s2c.play.SimulationDistanceS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Handle custom distances
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends WorldMixin {

    @Shadow
    @Final
    private ServerChunkManager chunkManager;

    @Shadow
    public abstract List<ServerPlayerEntity> getPlayers();

    @Unique
    private int bb$view_distance = 6;

    @Unique
    private int bb$simulation_distance = 6;

    @Unique
    @Override
    public void bb$setViewDistance(int view_distance) {

        if (this.bb$view_distance == view_distance) {
            return;
        }

        this.bb$view_distance = view_distance;

        this.chunkManager.applyViewDistance(view_distance);

        var update_packet = new ChunkLoadDistanceS2CPacket(view_distance);
        for (var player :this.getPlayers()) {
            player.networkHandler.sendPacket(update_packet);
        }
    }

    @Unique
    @Override
    public int bb$getViewDistance() {
        return this.bb$view_distance;
    }

    @Unique
    @Override
    public void bb$setSimulationDistance(int simulation_distance) {

        if (this.bb$simulation_distance == simulation_distance) {
            return;
        }

        this.bb$simulation_distance = simulation_distance;

        this.chunkManager.ticketManager.setSimulationDistance(simulation_distance);
        var update_packet = new SimulationDistanceS2CPacket(simulation_distance);

        for (var player :this.getPlayers()) {
            player.networkHandler.sendPacket(update_packet);
        }
    }

    @Unique
    @Override
    public int bb$getSimulationDistance() {
        return this.bb$simulation_distance;
    }

    @Inject(method = "addPlayer", at = @At("RETURN"))
    private void bb$onAddPlayer(ServerPlayerEntity player, CallbackInfo ci) {
        var update_packet = new SimulationDistanceS2CPacket(this.bb$simulation_distance);
        player.networkHandler.sendPacket(update_packet);

        var update_packet2 = new ChunkLoadDistanceS2CPacket(this.bb$view_distance);
        player.networkHandler.sendPacket(update_packet2);
    }
}
