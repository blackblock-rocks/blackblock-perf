package rocks.blackblock.perf.mixin.entity.parallel_ticking;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.entity.EntityLike;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.world.ServerEntityManager$Listener")
public abstract class ServerEntityManagerListenerMixin <T extends EntityLike> {

    @Shadow @Final private T entity;

    @Shadow public abstract void updateEntityPosition();

    @Inject(
            method = "updateEntityPosition",
            at = @At("HEAD"),
            cancellable = true
    )
    public void bb$onUpdatePosition(CallbackInfo ci) {

        ServerWorld world = (ServerWorld) ((Entity) this.entity).getWorld();

        var executor = world.getChunkManager().chunkLoadingManager.mainThreadExecutor;

        if (executor.isOnThread()) {
            return;
        }

        ci.cancel();

        executor.submit(() -> {
            this.updateEntityPosition();
        });

    }

}
