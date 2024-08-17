package rocks.blackblock.perf.mixin.reduce_alloc;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocks.blackblock.perf.dedup.Constants;

@Mixin(NbtList.class)
public class NbtListMixinForReducedAlloc {

    @ModifyReturnValue(method = "getIntArray", at = @At(value = "RETURN", ordinal = 1))
    private int[] redirectGetIntArray(int[] original) {
        return Constants.emptyIntArray;
    }

    @ModifyReturnValue(method = "getLongArray", at = @At(value = "RETURN", ordinal = 1))
    private long[] redirectGetLongArray(long[] original) {
        return Constants.emptyLongArray;
    }
}
