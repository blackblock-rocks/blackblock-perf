package rocks.blackblock.perf.mixin.no_palette_locking;

import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Removes the locking of paletted containers
 *
 * @author   ishland
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(PalettedContainer.class)
public class PalettedContainerMixin {

    /**
     * @author ishland
     * @reason removes locking
     */
    @Overwrite
    public void lock() {
        // no-op
    }

    /**
     * @author ishland
     * @reason removes locking
     */
    @Overwrite
    public void unlock() {
        // no-op
    }
}
