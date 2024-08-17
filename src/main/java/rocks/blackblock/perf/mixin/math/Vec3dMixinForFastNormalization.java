package rocks.blackblock.perf.mixin.math;

import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;

@Mixin(Vec3d.class)
public class Vec3dMixinForFastNormalization {

    @Mutable
    @Shadow
    @Final
    public double x;

    @Mutable
    @Shadow
    @Final
    public double y;

    @Mutable
    @Shadow
    @Final
    public double z;

    /**
     * @author Jelle De Loecker <jelle@elevenways.be>
     * @reason Use early exit
     */
    @Overwrite
    public Vec3d normalize() {

        double squared_length = x * x + y * y + z * z;

        if (squared_length < 1.0E-8) {
            return Vec3d.ZERO;
        }

        double inv_length = 1.0 / Math.sqrt(squared_length);
        return new Vec3d(x * inv_length, y * inv_length, z * inv_length);
    }
}
