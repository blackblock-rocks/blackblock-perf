package rocks.blackblock.perf.mixin.entity.bat_halloween;

import net.minecraft.entity.passive.BatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import rocks.blackblock.bib.util.BibTime;

/**
 * Don't check the date every time a bat is trying to spawn.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(BatEntity.class)
public class BatEntityMixin {

    /**
     * @author Jelle De Loecker <jelle@elevenways.be>
     * @reason Cache the date
     */
    @Overwrite
    private static boolean isTodayAroundHalloween() {
        return BibTime.IS_AROUND_HALLOWEEN;
    }
}
