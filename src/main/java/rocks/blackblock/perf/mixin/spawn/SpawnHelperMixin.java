package rocks.blackblock.perf.mixin.spawn;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import rocks.blackblock.bib.util.BibChunk;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.perf.spawn.CheckBelowCapPerWorld;
import rocks.blackblock.perf.spawn.DynamicSpawns;

/**
 * Add certain world-specific mobcap overrides
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(SpawnHelper.class)
public abstract class SpawnHelperMixin {

    /**
     * Always disable "rare" or "persistent" spawns
     * @since 0.1.0
     */
    @ModifyVariable(method = "spawn", at = @At("HEAD"), index = 5, argsOnly = true)
    private static boolean bb$neverSpawnPersistent(boolean shouldSpawnPersistent) {
        return false;
    }

    /**
     * Do some trickery to make it spawn less often
     * @since 0.1.0
     */
    @ModifyExpressionValue(
            method = "spawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/SpawnGroup;isRare()Z"
            )
    )
    private static boolean bb$shouldCancelSpawn(boolean original, ServerWorld world, @Local(ordinal = 0) SpawnGroup category) {

        if (category.getCapacity() <= 0) {
            return true;
        }

        int interval = category.bb$getSpawnInterval(world);
        BibPerf.Info info = world.bb$getPerformanceInfo();

        if (info.isBusy()) {
            interval += 1;

            if (info.isOverloaded()) {
                interval += 1;
            }
        }

        return interval > 1 && world.getTime() % interval != 0;
    }

    /**
     * Check the caps before actually spawning
     * @since 0.1.0
     */
    @Inject(
            method = "spawn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/SpawnHelper;spawnEntitiesInChunk(Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/SpawnHelper$Checker;Lnet/minecraft/world/SpawnHelper$Runner;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private static void preventSpawn(ServerWorld world, WorldChunk chunk, SpawnHelper.Info info, boolean spawnAnimals, boolean spawnMonsters, boolean rareSpawn, CallbackInfo ci, SpawnGroup[] var6, int var7, int var8, SpawnGroup spawnGroup) {
        var cap_info = (CheckBelowCapPerWorld) info;
        var is_below_cap = cap_info.bb$isBelowCap(world, spawnGroup, chunk.getPos());

        if (!is_below_cap) {
            ci.cancel();
            return;
        }

        var perf_info = world.bb$getPerformanceInfo();

        if (perf_info.isBusy()) {

            boolean has_active_players = DynamicSpawns.hasActivePlayersNear(world, chunk.getPos());

            if (has_active_players) {
                return;
            }

            // Don't spawn if there are no active players nearby and the server is busy
            if (perf_info.isOverloaded() || perf_info.isRandomlyDisabled()) {
                ci.cancel();
            }
        }
    }

    /**
     * Prevent loading chunks when checking nether fortress structures
     * @since 0.1.0
     */
    @Redirect(
            method = "shouldUseNetherFortressSpawns",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"
            )
    )
    private static BlockState bb$preventAddingTickets(ServerWorld world, BlockPos pos) {
        return BibChunk.getBlockStateIfLoaded(world, pos);
    }
}
