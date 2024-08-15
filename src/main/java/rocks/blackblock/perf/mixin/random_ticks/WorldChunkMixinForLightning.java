package rocks.blackblock.perf.mixin.random_ticks;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.tick.ChunkTickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.interfaces.chunk_ticking.CustomLightningTicks;

/**
 * Instead of using a random to determine if lightning should strike
 * every time the chunk is ticked, define when lightning strikes preemptively.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(WorldChunk.class)
public class WorldChunkMixinForLightning implements CustomLightningTicks {

    @Unique
    private int bb$lightning_tick;

    @Override
    @Unique
    public final int bb$shouldDoLightning(Random random, int thunder_chance) {
        if (this.bb$lightning_tick-- <= 0) {
            this.bb$lightning_tick = random.nextInt(thunder_chance) << 1;
            return 0;
        }
        return -1;
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/tick/ChunkTickScheduler;Lnet/minecraft/world/tick/ChunkTickScheduler;J[Lnet/minecraft/world/chunk/ChunkSection;Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;Lnet/minecraft/world/gen/chunk/BlendingData;)V",
        at = @At("RETURN")
    )
    private void bb$initLightingTick(World world, ChunkPos pos, UpgradeData upgradeData, ChunkTickScheduler<?> blockTickScheduler, ChunkTickScheduler<?> fluidTickScheduler, long inhabitedTime, ChunkSection[] sectionArrayInitializer, WorldChunk.EntityLoader entityLoader, BlendingData blendingData, CallbackInfo ci) {
        this.bb$lightning_tick = world.random.nextInt(100000) << 1;
    }
}
