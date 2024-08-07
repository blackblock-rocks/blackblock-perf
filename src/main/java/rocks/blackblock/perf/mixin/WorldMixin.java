package rocks.blackblock.perf.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.perf.thread.HasPerformanceInfo;

@Mixin(World.class)
public class WorldMixin implements HasPerformanceInfo {

    @Unique
    private final BibPerf.Info bb$performance_info = new BibPerf.Info((World) (Object) this);

    @Override
    public BibPerf.Info bb$getPerformanceInfo() {
        return this.bb$performance_info;
    }
}
