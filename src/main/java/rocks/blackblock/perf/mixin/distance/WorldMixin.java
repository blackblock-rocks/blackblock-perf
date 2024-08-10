package rocks.blackblock.perf.mixin.distance;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rocks.blackblock.perf.interfaces.distances.CustomDistances;

@Mixin(World.class)
public class WorldMixin implements CustomDistances {

    @Unique
    @Override
    public void bb$setViewDistance(int view_distance) {
        // no-op
    }

    @Unique
    @Override
    public int bb$getViewDistance() {
        return 9;
    }

    @Unique
    @Override
    public void bb$setSimulationDistance(int simulation_distance) {
        // no-op
    }

    @Unique
    @Override
    public int bb$getSimulationDistance() {
        return 9;
    }
}
