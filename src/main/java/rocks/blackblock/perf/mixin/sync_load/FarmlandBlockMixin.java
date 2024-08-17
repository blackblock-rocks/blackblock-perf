package rocks.blackblock.perf.mixin.sync_load;

import net.minecraft.block.FarmlandBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import rocks.blackblock.bib.util.BibChunk;

@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {

    /**
     * @author Jelle De Loecker <jelle@elevenways.be>
     * @reason Optimize FarmlandBlock nearby water lookup
     */
    @Overwrite
    private static boolean isWaterNearby(WorldView world, BlockPos pos) {
        int start_x = pos.getX() - 4;
        int start_y = pos.getY();
        int start_z = pos.getZ() - 4;

        BlockPos.Mutable mutable = pos.mutableCopy();

        for (int dz = 0; dz <= 8; dz++) {
            mutable.set(start_x, start_y, start_z + dz);

            for (int dx = 0; dx <= 8; dx++) {
                mutable.setX(start_x + dx);

                Chunk chunk = BibChunk.getChunkNow(world, mutable);

                if (chunk == null) {
                    continue;
                }

                for (int dy = 0; dy <= 1; dy++) {
                    mutable.setY(start_y + dy);
                    FluidState fluid = chunk.getBlockState(mutable).getFluidState();

                    if (fluid.isIn(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
