package rocks.blackblock.perf.mixin.sync_load;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.task.SleepTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.bib.util.BibChunk;

/**
 * Don't load chunks for finding a bed.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(SleepTask.class)
public class SleepTaskMixin {

    // Don't load chunks to find beds.
    @Inject(
        method = "shouldRun",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;getRegistryKey()Lnet/minecraft/registry/RegistryKey;",
            ordinal = 0
        )
    )
    private void bb$onlyProcessIfLoaded(ServerWorld world, LivingEntity owner, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) GlobalPos globalPos) {
        if (!BibChunk.isChunkLoaded(world, globalPos.pos())) {
            cir.setReturnValue(false);
        }
    }
}
