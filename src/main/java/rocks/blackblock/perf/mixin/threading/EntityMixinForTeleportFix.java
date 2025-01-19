package rocks.blackblock.perf.mixin.threading;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.PortalManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.perf.thread.DynamicThreads;

@Mixin(Entity.class)
public abstract class EntityMixinForTeleportFix {

    @Shadow
    public abstract @Nullable Entity teleportTo(TeleportTarget teleportTarget);

    @Shadow
    public abstract World getWorld();

    @Shadow
    protected abstract void tickPortalCooldown();

    @Shadow
    @Nullable public PortalManager portalManager;

    @Shadow
    public abstract boolean canUsePortals(boolean allowVehicles);

    @Shadow
    public abstract void resetPortalCooldown();

    @Shadow
    public abstract boolean canTeleportBetween(World from, World to);

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

    /**
     * The same way we put the actual teleport logic on the server thread,
     * we also have to put the portal teleportation preparation logic on
     * the server thread.
     */
    @Inject(method = "tickPortalTeleportation", at = @At("HEAD"), cancellable = true)
    private void onTickPortalTeleportation(CallbackInfo ci) {

        // Keep all the original logic if threading is disabled
        if (!DynamicThreads.THREADS_ENABLED) {
            return;
        }

        // Prevent the original method from running
        ci.cancel();

        var world = this.getWorld();

        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        this.tickPortalCooldown();

        if (this.portalManager == null) {
            return;
        }

        Entity self = (Entity) (Object) this;
        boolean canUsePortals = this.canUsePortals(false);
        boolean tickedPortal = this.portalManager.tick(serverWorld, self, canUsePortals);

        if (!tickedPortal) {

            if (this.portalManager.hasExpired()) {
                this.portalManager = null;
            }

            return;
        }

        this.resetPortalCooldown();

        world.getServer().execute(this::bb$continuePortalTeleportation);
    }

    /**
     * Actually continue the portal teleportation logic
     * (Should run on the server thread)
     */
    @Unique
    private void bb$continuePortalTeleportation() {

        var world = this.getWorld();

        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        TeleportTarget teleportTarget = this.portalManager.createTeleportTarget(serverWorld, (Entity) (Object) this);

        if (teleportTarget == null) {
            return;
        }

        ServerWorld targetWorld = teleportTarget.world();

        if (serverWorld.getServer().isWorldAllowed(targetWorld)
                && (targetWorld.getRegistryKey() == serverWorld.getRegistryKey() || this.canTeleportBetween(serverWorld, targetWorld))) {
            this.teleportTo(teleportTarget);
        }
    }
}
