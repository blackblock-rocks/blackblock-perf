package rocks.blackblock.perf.mixin.reduce_alloc;

import net.minecraft.nbt.NbtLongArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import rocks.blackblock.perf.dedup.Constants;

@Mixin(NbtLongArray.class)
public class NbtLongArrayMixinForReducedAlloc {

    @Shadow
    private long[] value;

    /**
     * @author QPCrummer
     * @reason Reduce Allocations
     */
    @Overwrite
    public void clear() {
        this.value = Constants.emptyLongArray;
    }
}
