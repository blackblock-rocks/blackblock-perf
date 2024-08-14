package rocks.blackblock.perf.mixin.density_cap_access;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.SpawnDensityCapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.spawn.DensityCapDelegate;

/**
 * Use an optimized map for the spawn group densities.
 *
 * @author   ishland
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(SpawnDensityCapper.DensityCap.class)
public abstract class DensityCapMixin {

    @Mutable
    @Shadow
    @Final
    public Object2IntMap<SpawnGroup> spawnGroupsToDensity;

    @Unique
    private final int[] bb$spawn_group_densities = new int[SpawnGroup.values().length];

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.spawnGroupsToDensity = DensityCapDelegate.delegateSpawnGroupDensities(bb$spawn_group_densities);
    }

    /**
     * @author ishland
     * @reason opt: replace with array access
     */
    @Overwrite
    public void increaseDensity(SpawnGroup spawnGroup) {
        this.bb$spawn_group_densities[spawnGroup.ordinal()] ++;
    }

    /**
     * @author ishland
     * @reason opt: replace with array access
     */
    @Overwrite
    public boolean canSpawn(SpawnGroup spawnGroup) {
        return this.bb$spawn_group_densities[spawnGroup.ordinal()] < spawnGroup.getCapacity();
    }
}
