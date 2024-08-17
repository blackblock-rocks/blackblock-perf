package rocks.blackblock.perf.mixin.entity.looking;

import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.*;

import java.util.Optional;

/**
 * Use fast square root approximation
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(LookControl.class)
public class LookControlMixin {

    @Shadow
    protected double x;

    @Shadow
    protected double y;

    @Shadow
    protected double z;

    @Shadow
    @Final
    protected MobEntity entity;

    /**
     * @reason Not a good reason yet
     * @author Jelle De Loecker <jelle@elevenways.be>
     */
    @Overwrite
    protected Optional<Float> getTargetPitch() {
        double d = this.x - this.entity.getX();
        double e = this.y - this.entity.getEyeY();
        double f = this.z - this.entity.getZ();
        double g = Math.sqrt(d * d + f * f);

        if (!(Math.abs(e) > 1.0E-5F) && !(Math.abs(g) > 1.0E-5F)) {
            return Optional.empty();
        }

        return Optional.of((float)(-(MathHelper.atan2(e, g) * 180 / Math.PI)));
    }
}
