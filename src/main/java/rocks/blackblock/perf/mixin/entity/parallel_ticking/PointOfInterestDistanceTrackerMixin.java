package rocks.blackblock.perf.mixin.entity.parallel_ticking;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.util.ConcurrentLong2ByteOpenHashMap;

@Mixin(PointOfInterestStorage.PointOfInterestDistanceTracker.class)
public class PointOfInterestDistanceTrackerMixin {

    @Mutable
    @Shadow
    @Final
    public Long2ByteMap distances;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.distances = new ConcurrentLong2ByteOpenHashMap();
    }
}
