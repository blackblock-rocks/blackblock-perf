package rocks.blackblock.perf.mixin.player_watching.optimize_player_lookups;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnDensityCapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.bib.util.BibPos;
import rocks.blackblock.perf.distance.AreaPlayerChunkWatchingManager;

import java.util.Map;
import java.util.function.Function;

@Mixin(value = SpawnDensityCapper.class, priority = 950)
public class SpawnDensityCapperMixinForPlayerLookups {

    @Shadow
    @Final
    private ServerChunkLoadingManager chunkLoadingManager;

    @Mutable
    @Shadow
    @Final
    public Map<ServerPlayerEntity, SpawnDensityCapper.DensityCap> playersToDensityCap;

    private static final Function<ServerPlayerEntity, SpawnDensityCapper.DensityCap> bb$new_density_cap = ignored -> new SpawnDensityCapper.DensityCap();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.playersToDensityCap = new Reference2ReferenceOpenHashMap<>();
    }

    @Unique
    private Object[] bb$getMobSpawnablePlayersArray(ChunkPos chunkPos) {
        final AreaPlayerChunkWatchingManager manager = this.chunkLoadingManager.bb$getAreaPlayerChunkWatchingManager();
        return manager.getPlayersInGeneralAreaMap(chunkPos.toLong());
    }

    /**
     * @author ishland
     * @reason optimize & reduce allocations
     */
    @Overwrite
    public void increaseDensity(ChunkPos chunkPos, SpawnGroup spawnGroup) {
        final double centerX = chunkPos.getCenterX();
        final double centerZ = chunkPos.getCenterZ();
        for(Object _player : this.bb$getMobSpawnablePlayersArray(chunkPos)) {
            if (_player instanceof ServerPlayerEntity player && !player.isSpectator() && !player.bb$ignoreDueToSystemLoad() && BibPos.getSquaredDistance(centerX, centerZ, player.getX(), player.getZ()) <= 16384.0D) {
                this.playersToDensityCap.computeIfAbsent(player, bb$new_density_cap).increaseDensity(spawnGroup);
            }
        }
    }

    /**
     * @author ishland
     * @reason optimize & reduce allocations
     */
    @Overwrite
    public boolean canSpawn(SpawnGroup spawnGroup, ChunkPos chunkPos) {
        final double centerX = chunkPos.getCenterX();
        final double centerZ = chunkPos.getCenterZ();
        for(Object _player : this.bb$getMobSpawnablePlayersArray(chunkPos)) {
            if (_player instanceof ServerPlayerEntity player && !player.isSpectator() && !player.bb$ignoreDueToSystemLoad() && BibPos.getSquaredDistance(centerX, centerZ, player.getX(), player.getZ()) <= 16384.0D) {
                SpawnDensityCapper.DensityCap densityCap = this.playersToDensityCap.get(player);
                if (densityCap == null || densityCap.canSpawn(spawnGroup)) {
                    return true;
                }
            }
        }

        return false;
    }
}
