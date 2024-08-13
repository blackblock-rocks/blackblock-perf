package rocks.blackblock.perf.mixin.sync_load;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import rocks.blackblock.bib.util.BibChunk;

import java.util.function.Supplier;

/**
 * Don't load chunks for raytracing.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {

    private ServerWorld bb$self = (ServerWorld) (Object) this;

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Override
    @NotNull
    public BlockHitResult raycast(RaycastContext context) {

        Vec3d to = context.getEnd();
        if (BibChunk.isChunkLoaded(this.bb$self, MathHelper.floor(to.x) >> 4, MathHelper.floor(to.z) >> 4)) {
            return super.raycast(context);
        } else {
            Vec3d vec3 = context.getStart().subtract(to);
            return BlockHitResult.createMissed(to, Direction.getFacing(vec3.x, vec3.y, vec3.z), BlockPos.ofFloored(to));
        }
    }
}
