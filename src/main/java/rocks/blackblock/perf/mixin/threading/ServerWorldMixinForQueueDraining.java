package rocks.blackblock.perf.mixin.threading;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.thread.DynamicThreads;

import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public class ServerWorldMixinForQueueDraining {

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerChunkManager;tick(Ljava/util/function/BooleanSupplier;Z)V",
            shift = At.Shift.BEFORE
        )
    )
    private void bb$beforeChunkManagerTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        DynamicThreads.drainOurLocalQueue();
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerChunkManager;tick(Ljava/util/function/BooleanSupplier;Z)V",
            shift = At.Shift.AFTER
        )
    )
    private void bb$afterChunkManagerTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        DynamicThreads.drainOurLocalQueue();
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerEntityManager;tick()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void bb$beforeEntityManagerTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        DynamicThreads.drainOurLocalQueue();
    }

    @Inject(
            method = "tick",
            at = @At("RETURN")
    )
    private void bb$afterTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        DynamicThreads.drainOurLocalQueue();
    }
}
