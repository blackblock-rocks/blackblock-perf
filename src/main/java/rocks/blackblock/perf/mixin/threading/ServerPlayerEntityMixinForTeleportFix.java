package rocks.blackblock.perf.mixin.threading;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.perf.thread.DynamicThreads;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixinForTeleportFix {

    @Shadow
    public abstract @Nullable Entity teleportTo(TeleportTarget teleportTarget);

    @Inject(method = "teleportTo", at = @At("HEAD"), cancellable = true)
    public void moveToWorld(TeleportTarget teleportTarget, CallbackInfoReturnable<Entity> cir) {

        World target = teleportTarget.world();

        if (!DynamicThreads.THREADS_ENABLED || DynamicThreads.onWorldThread(target)) {
            return;
        }

        // Execute the teleportation on the main thread
        // (Once all the dimensional threads have finished)
        target.getServer().execute(() -> {
            this.teleportTo(teleportTarget);
        });

        cir.setReturnValue(null);
    }
}
