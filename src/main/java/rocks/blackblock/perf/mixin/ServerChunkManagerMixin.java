package rocks.blackblock.perf.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.chunk.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import rocks.blackblock.perf.thread.DynamicThreads;
import rocks.blackblock.perf.thread.WithMutableThread;

@Mixin(value = ServerChunkManager.class, priority = 1001)
public abstract class ServerChunkManagerMixin extends ChunkManager implements WithMutableThread {

    @Shadow private Thread serverThread;

    @Override
    @Unique
    public Thread bb$getMainThread() {
        return this.serverThread;
    }

    @Override
    @Unique
    public void bb$setMainThread(Thread thread) {
        this.serverThread = thread;
    }

    @WrapOperation(method = "getChunk", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"))
    public Thread currentThread(Operation<Thread> original) {

        Thread thread = original.call();

        if (DynamicThreads.THREADS_ENABLED && DynamicThreads.ownsThread(thread)) {
            return this.serverThread;
        }

        return thread;
    }
}
