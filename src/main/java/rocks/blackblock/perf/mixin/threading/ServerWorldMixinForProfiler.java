package rocks.blackblock.perf.mixin.threading;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rocks.blackblock.perf.debug.BlackblockWorldProfiler;
import rocks.blackblock.perf.thread.WithBlackblockWorldProfiler;

/**
 * Let each world have their own profiler
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerWorld.class)
public class ServerWorldMixinForProfiler implements WithBlackblockWorldProfiler {

    @Unique
    private BlackblockWorldProfiler bb$profiler = new BlackblockWorldProfiler((ServerWorld) (Object) this);

    @Unique
    @Override
    public BlackblockWorldProfiler bb$getWorldProfiler() {
        return this.bb$profiler;
    }
}
