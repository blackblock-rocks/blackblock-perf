package rocks.blackblock.perf.mixin.entity.move_zero_velocity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skip the movement check if the bounding box didn't change and the velocity is zero
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(Entity.class)
public class EntityMixin {

    @Shadow
    private Box boundingBox;

    @Unique
    private boolean bb$bounding_box_changed = false;

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        if (!this.bb$bounding_box_changed && movement.equals(Vec3d.ZERO)) {
            ci.cancel();
            this.bb$bounding_box_changed = false;
        }
    }

    @Inject(method = "setBoundingBox", at = @At("HEAD"))
    private void onBoundingBoxChanged(Box boundingBox, CallbackInfo ci) {
        if (!this.boundingBox.equals(boundingBox)) this.bb$bounding_box_changed = true;
    }
}
