package rocks.blackblock.perf.mixin.spawn;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import rocks.blackblock.perf.spawn.CheckBelowCapPerWorld;

@Mixin(SpawnHelper.class)
public abstract class SpawnHelperMixin {

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
        }
    }
}
