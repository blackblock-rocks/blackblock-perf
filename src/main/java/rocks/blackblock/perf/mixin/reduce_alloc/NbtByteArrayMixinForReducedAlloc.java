package rocks.blackblock.perf.mixin.reduce_alloc;

import net.minecraft.nbt.NbtByteArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import rocks.blackblock.perf.dedup.Constants;

@Mixin(NbtByteArray.class)
public class NbtByteArrayMixinForReducedAlloc {

    @Shadow
    private byte[] value;

    /**
     * @author QPCrummer
     * @reason Reduce allocations
     */
    @Overwrite
    public void clear() {
        this.value = Constants.emptyByteArray;
    }
}
