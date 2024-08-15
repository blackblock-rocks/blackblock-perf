package rocks.blackblock.perf.mixin.threading;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.DummyProfiler;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Supplier;

/**
 * The profiler is not threadsafe,
 * so we need to make sure we only profile a single world.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixinForProfiler extends World {

    @Unique
    private boolean bb$is_main_world = this.getRegistryKey().equals(World.OVERWORLD);

    protected ServerWorldMixinForProfiler(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Override
    public Profiler getProfiler() {

        if (!this.bb$is_main_world) {
            return DummyProfiler.INSTANCE;
        }

        return super.getProfiler();
    }

    @Override
    public Supplier<Profiler> getProfilerSupplier() {
        return this::getProfiler;
    }
}
