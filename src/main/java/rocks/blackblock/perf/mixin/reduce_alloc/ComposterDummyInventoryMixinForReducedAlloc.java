package rocks.blackblock.perf.mixin.reduce_alloc;

import net.minecraft.block.ComposterBlock;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import rocks.blackblock.perf.dedup.Constants;

@Mixin(ComposterBlock.DummyInventory.class)
public class ComposterDummyInventoryMixinForReducedAlloc {
    /**
     * @author QPCrummer
     * @reason Reduce Allocations
     */
    @Overwrite
    public int[] getAvailableSlots(Direction side) {
        return Constants.emptyIntArray;
    }
}
