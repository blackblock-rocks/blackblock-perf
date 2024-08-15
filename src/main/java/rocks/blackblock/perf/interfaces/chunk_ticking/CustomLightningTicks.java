package rocks.blackblock.perf.interfaces.chunk_ticking;

import net.minecraft.util.math.random.Random;

public interface CustomLightningTicks {
    default int bb$shouldDoLightning(Random random, int thunder_chance) {
        return -1;
    }
}
