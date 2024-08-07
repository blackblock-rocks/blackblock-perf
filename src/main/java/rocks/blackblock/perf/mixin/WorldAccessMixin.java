package rocks.blackblock.perf.mixin;

import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.perf.thread.HasPerformanceInfo;

@Mixin(WorldAccess.class)
public interface WorldAccessMixin extends HasPerformanceInfo {

    @Override
    default BibPerf.Info bb$getPerformanceInfo() {
        return null;
    }
}
