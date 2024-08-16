package rocks.blackblock.perf.mixin.math;

import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Use the native Math functions instead,
 * which are all IntrinsicCandidates
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(MathHelper.class)
public class MathHelperMixinForIntrinsic {

    /**
     * @author QPCrummer
     * @reason Use Intrinsic instead
     */
    @Overwrite
    public static int floor(float value) {
        return (int) Math.floor(value);
    }

    /**
     * @author QPCrummer
     * @reason Use Intrinsic instead
     */
    @Overwrite
    public static int floor(double value) {
        return (int) Math.floor(value);
    }

    /**
     * @author QPCrummer
     * @reason Use Intrinsic instead
     */
    @Overwrite
    public static int ceil(float value) {
        return (int) Math.ceil(value);
    }

    /**
     * @author QPCrummer
     * @reason Use Intrinsic instead
     */
    @Overwrite
    public static int ceil(double value) {
        return (int) Math.ceil(value);
    }

    /**
     * @author QPCrummer
     * @reason Use Intrinsic instead
     */
    @Overwrite
    public static double absMax(double a, double b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }
}
