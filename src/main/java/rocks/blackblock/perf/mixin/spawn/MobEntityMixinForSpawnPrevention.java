package rocks.blackblock.perf.mixin.spawn;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.bib.util.BibPerf;

@Mixin(MobEntity.class)
public class MobEntityMixinForSpawnPrevention {

    @Unique
    private static final ThreadLocal<Integer> counter = ThreadLocal.withInitial(() -> 0);

    @Inject(
            method="canMobSpawn",
            at=@At("HEAD"),
            cancellable = true
    )
    private static void checkCanSpawn(EntityType<? extends MobEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random, CallbackInfoReturnable<Boolean> cir) {

        // Always allow dragons & withers
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) {
            return;
        }

        // Target spawners and reinforcements, those as often used for mob farms
        boolean is_spawner_or_reinforcement = spawnReason == SpawnReason.SPAWNER || spawnReason == SpawnReason.REINFORCEMENT;

        if (is_spawner_or_reinforcement) {

            BibPerf.Info info = world.bb$getPerformanceInfo();

            if (info == null) {
                return;
            }

            if (info.isRandomlyDisabled()) {
                cir.setReturnValue(false);
                return;
            }

            if (info.isBusy()) {
                int count = counter.get() % 3;
                counter.set(count + 1);

                // Cancel 1 in 3 spawns when server is busy
                if (count == 0) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }
}
