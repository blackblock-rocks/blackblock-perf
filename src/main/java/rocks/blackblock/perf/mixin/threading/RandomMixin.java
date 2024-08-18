package rocks.blackblock.perf.mixin.threading;

import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSeed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import rocks.blackblock.bib.random.ConcurrentRandom;
import rocks.blackblock.bib.random.XorShiftRandom;

@Mixin(Random.class)
public interface RandomMixin {
    /**
     * @author Jelle De Loecker <jelle@elevenways.be>
     * @reason Use our own ConcurrentRandom implementation
     */
    @Deprecated
    @Overwrite
    static Random createThreadSafe() {
        return new ConcurrentRandom(RandomSeed.getSeed());
    }

    /**
     * @author Jelle De Loecker <jelle@elevenways.be>
     * @reason Keep the local split thread-safe
     */
    @Overwrite
    static Random createLocal() {
        return new ConcurrentRandom(ThreadLocalRandom.current().nextLong());
    }

    /**
     * @author QPCrummer
     * @reason Use QPCrummer's implementation of XorShift
     */
    @Overwrite
    static Random create(long seed) {
        return new XorShiftRandom((int) seed);
    }
}
