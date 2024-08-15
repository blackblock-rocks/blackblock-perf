package rocks.blackblock.perf.mixin.spawn;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.perf.spawn.DynamicSpawns;

/**
 * Enforce the mobcap modifiers on Mob spawners
 * (This does not include trial spawners)
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(MobSpawnerLogic.class)
public abstract class MobSpawnerLogicMixin {

    @Shadow protected abstract void updateSpawns(World world, BlockPos pos);

    @Inject(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;getEntitiesByType(Lnet/minecraft/util/TypeFilter;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;"
            ),
            cancellable = true
    )
    private void bb$enforceMobcap(ServerWorld world, BlockPos pos, CallbackInfo ci, @Local(ordinal = 0) Entity entity) {

        BibPerf.Info info = world.bb$getPerformanceInfo();

        if (info.isRandomlyDisabled()) {
            ci.cancel();
            return;
        }

        if (info.isOverloaded() && !DynamicSpawns.hasActivePlayersNear(world, new ChunkPos(pos))) {
            ci.cancel();
            return;
        }

        // @TODO: Add spawner-specific logic that allows for more spawns
        /*
        boolean can_spawn = DynamicSpawns.canSpawn(entity, world, pos);

        if (!can_spawn) {
            this.updateSpawns(world, pos);
            ci.cancel();
        }
        */
    }
}
