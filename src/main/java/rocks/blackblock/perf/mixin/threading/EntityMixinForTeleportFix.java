package rocks.blackblock.perf.mixin.threading;

import net.minecraft.entity.Entity;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.perf.thread.DynamicThreads;

@Mixin(Entity.class)
public abstract class EntityMixinForTeleportFix {

    @Shadow public abstract @Nullable Entity teleportTo(TeleportTarget teleportTarget);

    /**
     * Force teleportations to happen on the main server thread,
     * after all the worlds have ticked concurrently.
     * This way we avoid any CME or deadlocks without too much hassle.
     */
    @Inject(method = "teleportTo", at = @At("HEAD"), cancellable = true)
    public void moveToWorld(TeleportTarget teleportTarget, CallbackInfoReturnable<Entity> cir) {

        World target = teleportTarget.world();

        if (!DynamicThreads.THREADS_ENABLED || DynamicThreads.onWorldThread(target)) {
            return;
        }

        // Executing the teleport on the target thread immediately only works
        // if the entity does not have any passengers.
        // So we don't do it like this anymore, and instead defer it to the main thread
        //DynamicThreads.sendToWorldThread(target, () -> this.teleportTo(teleportTarget));

        // Execute the teleportation on the main thread
        // (Once all the dimensional threads have finished)
        target.getServer().execute(() -> {
            this.teleportTo(teleportTarget);
        });

        cir.setReturnValue(null);
    }
}
