package rocks.blackblock.perf.mixin.entity.activation_range;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.activation_range.ActivationRange;
import rocks.blackblock.perf.activation_range.DynamicActivationRange;
import rocks.blackblock.perf.interfaces.activation_range.DeactivatableEntity;
import rocks.blackblock.perf.interfaces.activation_range.InactiveTickable;

/**
 * Make entities deactivatable
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(Entity.class)
public class EntityMixin implements DeactivatableEntity, InactiveTickable {

    @Shadow private World world;
    @Unique
    private final Entity bb$self = (Entity) (Object) this;

    @Unique
    private ActivationRange bb$activation_range;

    @Unique
    private boolean bb$excluded_from_activation_range;

    @Unique
    private int bb$activated_until_tick;

    @Unique
    private int bb$immune_until_tick;

    @Unique
    private int bb$potential_tick_count;

    @Unique
    private boolean bb$inactive;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void bb$setupActivationStates(EntityType<?> type, World world, CallbackInfo ci) {
        this.bb$activation_range = DynamicActivationRange.getActivationRange(this.bb$self);
        this.bb$excluded_from_activation_range = world == null || this.bb$activation_range == null;
    }

    @Inject(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;adjustMovementForPiston(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
                    shift = At.Shift.BEFORE
            )
    )
    public void bb$onPistonMove(MovementType moverType, Vec3d vec3, CallbackInfo ci) {
        MinecraftServer server = this.world.getServer();
        if (server != null) {
            final int ticks = server.getTicks() + 20;
            this.bb$activated_until_tick = Math.max(this.bb$activated_until_tick, ticks);
            this.bb$immune_until_tick = Math.max(this.bb$immune_until_tick, ticks);
        }
    }

    // Prevent inactive entities from getting extreme velocities.
    @Inject(method = "addVelocity(DDD)V", at = @At("HEAD"), cancellable = true)
    public void bb$ignorePushingWhileInactive(double x, double y, double z, CallbackInfo ci) {
        if (this.bb$inactive) {
            ci.cancel();
        }
    }

    @Override
    @Unique
    public ActivationRange bb$getActivationRange() {
        return this.bb$activation_range;
    }

    @Override
    @Unique
    public boolean bb$isExcludedFromDynamicActivationRange() {
        return this.bb$excluded_from_activation_range;
    }

    @Override
    @Unique
    public int bb$getActivatedUntilTick() {
        return this.bb$activated_until_tick;
    }

    @Override
    @Unique
    public void bb$setActivatedUntilTick(int tick) {
        this.bb$activated_until_tick = tick;
    }

    @Override
    @Unique
    public int bb$getImmuneUntilTick() {
        return this.bb$immune_until_tick;
    }

    @Override
    @Unique
    public void bb$setImmuneUntilTick(int tick) {
        this.bb$immune_until_tick = tick;
    }

    @Override
    @Unique
    public boolean bb$isInactive() {
        return this.bb$inactive;
    }

    @Override
    @Unique
    public void bb$setInactive(boolean inactive) {
        this.bb$inactive = inactive;
    }

    @Override
    @Unique
    public int bb$getPotentialTickCount() {
        return this.bb$potential_tick_count;
    }

    @Override
    @Unique
    public void bb$incrementPotentialTickCount() {
        this.bb$potential_tick_count++;
    }

    @Override
    public void bb$inactiveTick() {
        // no-op
    }
}
