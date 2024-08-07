package rocks.blackblock.perf.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rocks.blackblock.perf.thread.WithMutableThread;

@Mixin(World.class)
public interface WorldAccessor extends WithMutableThread {

    @Accessor("thread")
    @Override
    void bb$setMainThread(Thread thread);

    @Accessor("thread")
    @Override
    Thread bb$getMainThread();
}
