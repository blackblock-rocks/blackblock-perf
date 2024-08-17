package rocks.blackblock.perf.mixin.entity.parallel_ticking;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.light.LevelPropagator;
import net.minecraft.world.chunk.light.PendingUpdateQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelPropagator.class)
public abstract class LevelPropagatorMixin {

    @Shadow @Final private PendingUpdateQueue pendingUpdateQueue;

    @Shadow protected abstract int getLevel(long id);

    @Shadow @Final protected int levelCount;

    @Shadow @Final private Long2ByteMap pendingUpdates;

    @Shadow protected abstract void setLevel(long id, int level);

    @Shadow protected abstract void propagateLevel(long id, int level, boolean decrease);

    @Shadow protected abstract int calculateLevel(int a, int b);

    @Shadow private volatile boolean hasPendingUpdates;

    @Inject(method = "applyPendingUpdates", at = @At("HEAD"), cancellable = true)
    private void bb$onApplyPendingUpdates(int maxSteps, CallbackInfoReturnable<Integer> cir) {
        int result = this.bb$originalAplyPendingUpdates(maxSteps);
        cir.setReturnValue(result);
    }

    @Unique
    protected synchronized final int bb$originalAplyPendingUpdates(int maxSteps) {
        if (this.pendingUpdateQueue.isEmpty()) {
            return maxSteps;
        } else {
            while (!this.pendingUpdateQueue.isEmpty() && maxSteps > 0) {
                maxSteps--;
                long l = this.pendingUpdateQueue.dequeue();
                int i = MathHelper.clamp(this.getLevel(l), 0, this.levelCount - 1);
                int j = this.pendingUpdates.remove(l) & 255;
                if (j < i) {
                    this.setLevel(l, j);
                    this.propagateLevel(l, j, true);
                } else if (j > i) {
                    this.setLevel(l, this.levelCount - 1);
                    if (j != this.levelCount - 1) {
                        this.pendingUpdateQueue.enqueue(l, this.calculateLevel(this.levelCount - 1, j));
                        this.pendingUpdates.put(l, (byte)j);
                    }

                    this.propagateLevel(l, i, false);
                }
            }

            this.hasPendingUpdates = !this.pendingUpdateQueue.isEmpty();
            return maxSteps;
        }
    }

}
