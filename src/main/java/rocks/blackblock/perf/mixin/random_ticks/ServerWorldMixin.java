package rocks.blackblock.perf.mixin.random_ticks;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.perf.interfaces.chunk_ticking.ResettableIceAndSnowTicks;

import java.util.function.Supplier;

/**
 * Make lightning & ice and snow random ticks less resource intensive
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(value = ServerWorld.class, priority = 900)
public abstract class ServerWorldMixin extends World implements ResettableIceAndSnowTicks {

    @Unique
    private int bb$current_ice_and_snow_ticks = 0;

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Redirect(
        method = "tickChunk",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I",
            ordinal = 0
        )
    )
    private int bb$replaceLightningCheck(Random random, int thunder_chance, WorldChunk chunk, int random_tick_speed) {
        return chunk.bb$shouldDoLightning(random, thunder_chance);
    }

    @Redirect(
        method = "tickChunk",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I",
            ordinal = 1
        )
    )
    private int bb$replaceIceAndSnowCheck(Random random, int i) {
        return this.bb$current_ice_and_snow_ticks++ & 15;
    }

    @Override
    @Unique
    public void bb$resetIceAndSnowTick() {
        this.bb$current_ice_and_snow_ticks = this.random.nextInt(16);
    }
}
