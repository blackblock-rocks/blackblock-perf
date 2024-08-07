package rocks.blackblock.perf.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.world.TeleportTarget;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.perf.thread.DynamicThreads;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract @Nullable Entity teleportTo(TeleportTarget teleportTarget);

    /**
     * Force teleportations to happen on the main server thread,
     * after all the worlds have ticked concurrently.
     * This way we avoid any CME or deadlocks without too much hassle.
     */
    @Inject(method = "teleportTo", at = @At("HEAD"), cancellable = true)
    public void moveToWorld(TeleportTarget teleportTarget, CallbackInfoReturnable<Entity> cir) {

        if (!DynamicThreads.THREADS_ENABLED) {
            return;
        }

        if (DynamicThreads.ownsThread(Thread.currentThread())) {
            teleportTarget.world().getServer().execute(() -> {
                this.teleportTo(teleportTarget);
            });

            cir.setReturnValue(null);
        }
    }
}
