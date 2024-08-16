package rocks.blackblock.perf.mixin.collections;

import net.minecraft.util.collection.SortedArraySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Optimizes the removeIf method of SortedArraySet:
 * - Performing a single pass over the array
 * - Minimizing memory writes
 * - Avoiding creation of iterator objects
 * - Efficiently shifting remaining elements
 *
 * @author   PaperMC team
 * @since    0.1.0
 */
@Mixin(SortedArraySet.class)
public abstract class SortedArraySetMixin<T> extends AbstractSet<T> {

    @Shadow
    int size;

    @Shadow
    T[] elements;

    // Paper start - optimise removeIf
    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        // prev. impl used an iterator, which could be n^2 and creates garbage
        int i = 0, len = this.size;
        T[] backingArray = this.elements;

        for (;;) {
            if (i >= len) {
                return false;
            }
            if (!filter.test(backingArray[i])) {
                ++i;
                continue;
            }
            break;
        }

        // we only want to write back to backingArray if we really need to

        int lastIndex = i; // this is where new elements are shifted to

        for (; i < len; ++i) {
            T curr = backingArray[i];
            if (!filter.test(curr)) { // if test throws we're screwed
                backingArray[lastIndex++] = curr;
            }
        }

        // cleanup end
        Arrays.fill(backingArray, lastIndex, len, null);
        this.size = lastIndex;
        return true;
    }
}
