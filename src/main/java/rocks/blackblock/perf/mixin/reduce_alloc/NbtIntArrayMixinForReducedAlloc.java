package rocks.blackblock.perf.mixin.reduce_alloc;

import net.minecraft.nbt.NbtIntArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import rocks.blackblock.perf.dedup.Constants;

@Mixin(NbtIntArray.class)
public class NbtIntArrayMixinForReducedAlloc {

    @Shadow
    private int[] value;

    /**
     * @author QPCrummer
     * @reason Reduce Allocations
     */
    @Overwrite
    public void clear() {
        this.value = Constants.emptyIntArray;
    }
}
