package rocks.blackblock.perf.mixin.distance;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket;
import net.minecraft.server.network.ChunkFilter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.distance.AreaPlayerChunkWatchingManager;
import rocks.blackblock.perf.interfaces.distances.HasAreaPlayerChunkWatchingManager;

import java.util.List;

@Mixin(ServerChunkLoadingManager.class)
public abstract class ServerChunkLoadingManagerMixin implements HasAreaPlayerChunkWatchingManager {

    @Shadow
    private static void untrack(ServerPlayerEntity player, ChunkPos pos) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void track(ServerPlayerEntity player, ChunkPos pos);

    @Shadow
    public int watchDistance;

    @Shadow
    @Final
    private ServerChunkLoadingManager.TicketManager ticketManager;

    @Shadow
    protected abstract boolean canTickChunk(ServerPlayerEntity player, ChunkPos pos);

    @Shadow
    protected abstract void updateWatchedSection(ServerPlayerEntity player);

    @Shadow
    abstract int getViewDistance(ServerPlayerEntity player);

    // Our custom area player chunk watching manager
    @Unique
    private AreaPlayerChunkWatchingManager bb$area_player_chunk_watching_manager;

    /**
     * Initialize the area player chunk watching manager on initialization
     * @since    0.1.0
     */
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;setViewDistance(I)V"))
    private void redirectNewPlayerChunkWatchingManager(CallbackInfo ci) {
        this.bb$area_player_chunk_watching_manager = new AreaPlayerChunkWatchingManager(
                (player, chunkX, chunkZ) -> this.track(player, new ChunkPos(chunkX, chunkZ)),
                (player, chunkX, chunkZ) -> untrack(player, new ChunkPos(chunkX, chunkZ)),
                (ServerChunkLoadingManager) (Object) this);
    }

    /**
     * Return the area player chunk watching manager
     * @since    0.1.0
     */
    @Override
    @Unique
    public AreaPlayerChunkWatchingManager bb$getAreaPlayerChunkWatchingManager() {
        return this.bb$area_player_chunk_watching_manager;
    }

    /**
     * Tick the area player chunk watching manager when the server ticks
     * @since    0.1.0
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(CallbackInfo ci) {
        this.bb$area_player_chunk_watching_manager.tick();
    }

    /**
     * Propagate the view distance to the area player chunk watching manager
     * @since    0.1.0
     */
    @Inject(method = "setViewDistance", at = @At("RETURN"))
    private void onSetViewDistance(CallbackInfo ci) {
        this.bb$area_player_chunk_watching_manager.setWatchDistance(this.watchDistance);
    }

    /**
     * @author ishland
     * @reason use array for iteration & use squares as cylinders are expensive
     */
    @Overwrite
    public List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
        final AreaPlayerChunkWatchingManager watchingManager = this.bb$area_player_chunk_watching_manager;

        Object[] set = watchingManager.getPlayersWatchingChunkArray(chunkPos.toLong());
        ImmutableList.Builder<ServerPlayerEntity> builder = ImmutableList.builder();

        for (Object __player : set) {
            if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
                ChunkSectionPos watchedPos = serverPlayerEntity.getWatchedSection();
                int chebyshevDistance = Math.max(Math.abs(watchedPos.getSectionX() - chunkPos.x), Math.abs(watchedPos.getSectionZ() - chunkPos.z));
                if (chebyshevDistance > this.watchDistance) {
                    continue;
                }
                if (!serverPlayerEntity.networkHandler.chunkDataSender.isInNextBatch(chunkPos.toLong()) &&
                        (!onlyOnWatchDistanceEdge || chebyshevDistance == this.watchDistance)) {
                    builder.add(serverPlayerEntity);
                }
            }
        }

        return builder.build();
    }

    /**
     * @author ishland
     * @reason use array for iteration
     */
    @Overwrite
    public List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos pos) {
        long l = pos.toLong();
        if (!this.ticketManager.shouldTick(l)) {
            return List.of();
        } else {
            ImmutableList.Builder<ServerPlayerEntity> builder = ImmutableList.builder();

            for (Object __player : this.bb$area_player_chunk_watching_manager.getPlayersInGeneralAreaMap(l)) {
                if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
                    if (this.canTickChunk(serverPlayerEntity, pos)) {
                        builder.add(serverPlayerEntity);
                    }
                }
            }

            return builder.build();
        }
    }

    /**
     * @author ishland
     * @reason use array for iteration
     */
    @Overwrite
    public boolean shouldTick(ChunkPos pos) {
        long l = pos.toLong();
        if (!this.ticketManager.shouldTick(l)) {
            return false;
        } else {
            for (Object __player : this.bb$area_player_chunk_watching_manager.getPlayersInGeneralAreaMap(l)) {
                if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
                    if (this.canTickChunk(serverPlayerEntity, pos)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Inject(method = "handlePlayerAddedOrRemoved", at = @At("HEAD"))
    private void onHandlePlayerAddedOrRemoved(ServerPlayerEntity player, boolean added, CallbackInfo ci) {
        if (added) {
            this.bb$updateWatchedSection(player);
            this.bb$area_player_chunk_watching_manager.add(player, player.getWatchedSection().toChunkPos().toLong());
        } else {
            this.bb$area_player_chunk_watching_manager.remove(player);
        }
    }

    @Inject(method = "updatePosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;updateWatchedSection(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
    private void onPlayerSectionChange(ServerPlayerEntity player, CallbackInfo ci) {
        this.bb$updateWatchedSection(player);
        this.bb$area_player_chunk_watching_manager.movePlayer(player.getWatchedSection().toChunkPos().toLong(), player);
    }

    @Unique
    private void bb$updateWatchedSection(ServerPlayerEntity player) {
        this.updateWatchedSection(player);
        player.networkHandler.sendPacket(new ChunkRenderDistanceCenterS2CPacket(player.getWatchedSection().getSectionX(), player.getWatchedSection().getSectionZ()));
        player.setChunkFilter(ChunkFilter.cylindrical(player.getWatchedSection().toChunkPos(), this.getViewDistance(player)));
    }

}
