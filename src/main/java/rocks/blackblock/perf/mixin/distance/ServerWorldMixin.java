package rocks.blackblock.perf.mixin.distance;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.WolfBegGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SimulationDistanceS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.*;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.bib.util.BibPos;
import rocks.blackblock.perf.distance.AreaPlayerChunkWatchingManager;
import rocks.blackblock.perf.interfaces.distances.CustomDistances;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Handle custom distances
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements EntityLookupView, CustomDistances, StructureWorldAccess {

    @Shadow
    @Final
    private ServerChunkManager chunkManager;

    @Shadow
    public abstract List<ServerPlayerEntity> getPlayers();

    @Unique
    private int bb$max_view_distance = 6;

    @Unique
    private int bb$simulation_distance = 6;

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    /**
     * Receive & handle a new view distance value,
     * probably from a dynamic setting
     *
     * @since    0.1.0
     */
    @Unique
    @Override
    public void bb$setMaxViewDistance(int view_distance) {

        if (this.bb$max_view_distance == view_distance) {
            return;
        }

        this.bb$max_view_distance = view_distance;

        // Inform the chunk manager of the new view distance
        // (Even though we override most of the vanilla code)
        this.chunkManager.applyViewDistance(view_distance);

        for (var player :this.getPlayers()) {
            player.bb$recalculatePersonalViewDistance();
        }
    }

    @Unique
    @Override
    public int bb$getMaxViewDistance() {
        return this.bb$max_view_distance;
    }

    /**
     * Receive & handle a new simulation distance value,
     * probably from a dynamic setting
     *
     * @since    0.1.0
     */
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

    /**
     * Use the AreaPlayerChunkWatchingManager to find the closest player
     *
     * @since    0.1.0
     */
    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(double x, double y, double z, double maxDistance, @Nullable Predicate<Entity> targetPredicate) {

        final AreaPlayerChunkWatchingManager playerChunkWatchingManager = this.chunkManager.chunkLoadingManager.bb$getAreaPlayerChunkWatchingManager();

        final int chunkX = ChunkSectionPos.getSectionCoord(x);
        final int chunkZ = ChunkSectionPos.getSectionCoord(z);

        if (AreaPlayerChunkWatchingManager.GENERAL_PLAYER_AREA_MAP_DISTANCE * 16 < maxDistance || maxDistance < 0.0D) {
            // too far away for this to handle
            return super.getClosestPlayer(x, y, z, maxDistance, targetPredicate);
        }

        final Object[] playersWatchingChunkArray = playerChunkWatchingManager.getPlayersInGeneralAreaMap(BibPos.toLong(chunkX, chunkZ));

        ServerPlayerEntity nearestPlayer = null;
        double nearestDistance = maxDistance * maxDistance; // maxDistance < 0.0D handled above
        for (Object __player : playersWatchingChunkArray) {
            if (__player instanceof ServerPlayerEntity player) {
                if (targetPredicate == null || targetPredicate.test(player)) {
                    final double distance = player.squaredDistanceTo(x, y, z);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestPlayer = player;
                    }
                }
            }
        }

        return nearestPlayer;
    }

    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(TargetPredicate targetPredicate, LivingEntity entity) {
        return this.getClosestPlayer(targetPredicate, entity, entity.getX(), entity.getY(), entity.getZ());
    }

    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(TargetPredicate targetPredicate, LivingEntity entity, double x, double y, double z) {
        final Object[] playersWatchingChunkArray = this.bb$getPlayersWatchingChunkArray(x, z);

        ServerPlayerEntity nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        ServerWorld self = (ServerWorld) (Object) this;
        for (Object __player : playersWatchingChunkArray) {
            if (__player instanceof ServerPlayerEntity player) {
                if (targetPredicate == null || targetPredicate.test(self, entity, player)) {
                    final double distance = player.squaredDistanceTo(x, y, z);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestPlayer = player;
                    }
                }
            }
        }

        return nearestPlayer;
    }

    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(TargetPredicate targetPredicate, double x, double y, double z) {
        return this.getClosestPlayer(targetPredicate, null, x, y, z);
    }

    @Unique
    private Object[] bb$getPlayersWatchingChunkArray(double x, double z) {
        final AreaPlayerChunkWatchingManager playerChunkWatchingManager = this.chunkManager.chunkLoadingManager.bb$getAreaPlayerChunkWatchingManager();
        final int chunkX = ChunkSectionPos.getSectionCoord(x);
        final int chunkZ = ChunkSectionPos.getSectionCoord(z);

        // no maxDistance here so just search within the range,
        // and hopefully it works

        final Object[] playersWatchingChunkArray = playerChunkWatchingManager.getPlayersInGeneralAreaMap(BibPos.toLong(chunkX, chunkZ));
        return playersWatchingChunkArray;
    }

    @Override
    public boolean isPlayerInRange(double x, double y, double z, double range) {

        final AreaPlayerChunkWatchingManager playerChunkWatchingManager = this.chunkManager.chunkLoadingManager.bb$getAreaPlayerChunkWatchingManager();
        final int chunkX = ChunkSectionPos.getSectionCoord(x);
        final int chunkZ = ChunkSectionPos.getSectionCoord(z);

        if (AreaPlayerChunkWatchingManager.GENERAL_PLAYER_AREA_MAP_DISTANCE * 16 < range) // too far away for this to handle
            return super.isPlayerInRange(x, y, z, range);

        final Object[] playersWatchingChunkArray = playerChunkWatchingManager.getPlayersWatchingChunkArray(BibPos.toLong(chunkX, chunkZ));

        double rangeSquared = range * range;

        for (Object __player : playersWatchingChunkArray) {
            if (__player instanceof ServerPlayerEntity player) {
                if (!player.isSpectator() && player.isAlive()) {
                    final double distance = player.squaredDistanceTo(x, y, z);
                    if (range < 0.0 || distance < rangeSquared) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Send the correct simulation & view distances to the player
     * when they join the server
     *
     * @since    0.1.0
     */
    @Inject(method = "addPlayer", at = @At("RETURN"))
    private void bb$onAddPlayer(ServerPlayerEntity player, CallbackInfo ci) {
        var update_packet = new SimulationDistanceS2CPacket(this.bb$simulation_distance);
        player.networkHandler.sendPacket(update_packet);

        player.bb$recalculatePersonalViewDistance();
    }
}
