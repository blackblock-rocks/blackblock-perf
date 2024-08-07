package rocks.blackblock.perf.mixin.spawn;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocks.blackblock.perf.spawn.DynamicSpawns;

/**
 * Prevent the Nether Portal from spawning too many mobs
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {

    @ModifyExpressionValue(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;allowsSpawning(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/EntityType;)Z"
            )
    )
    private boolean bb$enforceMobCap(boolean isValidSpawn, BlockState state, ServerWorld world, BlockPos pos, Random random) {
        return isValidSpawn && DynamicSpawns.canSpawn(
                EntityType.ZOMBIFIED_PIGLIN.getSpawnGroup(),
                world,
                pos
        );
    }
}
