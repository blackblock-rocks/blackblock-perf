package rocks.blackblock.perf.mixin.fast_biome_access;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.bib.util.BibChunk;

/**
 * Even though we already optimized the biome access,
 * we can use an even faster implementation for the spawn helper.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(SpawnHelper.class)
public class SpawnHelperMixin {

    @Redirect(
        method = {
            "getSpawnEntries",
            "pickRandomSpawnEntry"
        },
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;getBiome(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/registry/entry/RegistryEntry;"
        )
    )
    private static RegistryEntry<Biome> bb$roughBiomeLookup(ServerWorld world, BlockPos pos) {
        return BibChunk.getRoughBiome(world, pos);
    }
}
