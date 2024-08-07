package rocks.blackblock.perf.mixin.spawn;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.spawn.CustomSpawnGroupLimits;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = SpawnGroup.class, priority = 900)
public class SpawnGroupMixin implements CustomSpawnGroupLimits {

    @Mutable
    @Shadow
    @Final
    private int capacity;

    @Unique
    private int bb$original_capacity;

    @Unique
    private int bb$original_spawn_interval;

    @Unique
    private Map<World, Integer> bb$base_capacity = new ConcurrentHashMap<>();

    @Unique
    private Map<World, Integer> bb$calculated_capacity = new ConcurrentHashMap<>();

    @Unique
    private Map<World, Integer> bb$spawn_interval = new ConcurrentHashMap<>();

    @Unique
    private Map<World, Double> bb$capacity_modifier = new ConcurrentHashMap<>();

    /**
     * Remember the original values
     * @since 0.1.0
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void bb$onInit(String string, int i, String name, int spawnCap, boolean peaceful, boolean rare, int immediateDespawnRange, CallbackInfo ci) {
        this.bb$original_capacity = spawnCap;
        this.bb$original_spawn_interval = rare ? 400 : 1;
    }

    @Override
    public int bb$getOriginalCapacity() {
        return this.bb$original_capacity;
    }

    @Override
    public int bb$getSpawnInterval(World world) {
        return this.bb$spawn_interval.getOrDefault(world, this.bb$original_spawn_interval);
    }

    @Override
    public void bb$setCapacityModifier(World world, double modifier) {

        if (this.bb$original_capacity < 1) {
            return;
        }

        this.bb$capacity_modifier.put(world, modifier);
        int base_capacity = this.bb$getBaseCapacity(world);
        int calculated_capacity = (int) (base_capacity * modifier);

        if (calculated_capacity < 1) {
            calculated_capacity = 1;
        }

        this.bb$calculated_capacity.put(world, calculated_capacity);
    }

    @Override
    public void bb$setSpawnInterval(World world, int interval) {
        this.bb$spawn_interval.put(world, interval);
    }

    @Override
    public void bb$setBaseCapacity(World world, int capacity) {
        this.bb$base_capacity.put(world, capacity);
    }

    @Override
    public int bb$getBaseCapacity(World world) {
        return this.bb$base_capacity.getOrDefault(world, this.bb$original_capacity);
    }

    @Override
    public int bb$getCapacity(World world) {
        return this.bb$calculated_capacity.getOrDefault(world, this.bb$original_capacity);
    }
}
