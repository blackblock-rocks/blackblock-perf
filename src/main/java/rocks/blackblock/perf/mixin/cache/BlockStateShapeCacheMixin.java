package rocks.blackblock.perf.mixin.cache;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.SideShapeType;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Each call to an Enum's `values()` method is quite costly:
 * it always creates a totally new array, and copies the values over.
 *
 * This mixin caches those 2 values,
 * and prevents +/- 450.000 allocations
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(AbstractBlock.AbstractBlockState.ShapeCache.class)
public class BlockStateShapeCacheMixin {

    @Unique
    private static final SideShapeType[] MF_BLOCK_VOXEL_SHAPES = SideShapeType.values();

    @Unique
    private static final Direction.Axis[] DIRECTION_AXIS_VALUES = Direction.Axis.values();

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/SideShapeType;values()[Lnet/minecraft/block/SideShapeType;"
        )
    )
    private SideShapeType[] getVoxelShapeValues() {
        return MF_BLOCK_VOXEL_SHAPES;
    }

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/Direction$Axis;values()[Lnet/minecraft/util/math/Direction$Axis;"
        )
    )
    private Direction.Axis[] getDirectionAxisValues() {
        return DIRECTION_AXIS_VALUES;
    }
}
