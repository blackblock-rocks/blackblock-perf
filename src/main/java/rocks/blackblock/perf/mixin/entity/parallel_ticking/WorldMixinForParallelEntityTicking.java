package rocks.blackblock.perf.mixin.entity.parallel_ticking;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rocks.blackblock.perf.interfaces.threading.ParallelEntityTicker;
import rocks.blackblock.perf.thread.ThreadPool;

@Mixin(World.class)
public class WorldMixinForParallelEntityTicking implements ParallelEntityTicker {

    @Unique
    private ThreadPool bb$entity_thread_pool = new ThreadPool(4);

    @Override
    @Unique
    public ThreadPool bb$getEntityThreadPool() {
        return this.bb$entity_thread_pool;
    }

}
