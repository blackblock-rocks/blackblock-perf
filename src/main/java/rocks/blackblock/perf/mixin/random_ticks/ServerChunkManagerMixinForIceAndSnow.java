package rocks.blackblock.perf.mixin.random_ticks;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkManager.class)
public class ServerChunkManagerMixinForIceAndSnow {

    @Shadow
    @Final
    ServerWorld world;

    // Hook into the "pollingChunks" profiler push
    @Inject(
        method = "tickChunks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V",
            ordinal = 0
        )
    )
    private void bb$resetIceAndSnowTick(CallbackInfo ci) {
        this.world.bb$resetIceAndSnowTick();
    }
}
