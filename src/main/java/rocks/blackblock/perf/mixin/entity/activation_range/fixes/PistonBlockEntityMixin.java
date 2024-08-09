package rocks.blackblock.perf.mixin.entity.activation_range.fixes;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fix items getting stuck in slime pushed by a piston
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(PistonBlockEntity.class)
public class PistonBlockEntityMixin {

    @Inject(
            method = "pushEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;setVelocity(DDD)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void bb$onPushEntity(World world, BlockPos pos, float f, PistonBlockEntity blockEntity, CallbackInfo ci, @Local(ordinal = 0) Entity entity) {
        MinecraftServer server = world.getServer();
        if (server != null) {
            final int tick = server.getTicks() + 10;
            entity.bb$setActivatedUntilTick(Math.max(entity.bb$getActivatedUntilTick(), tick));
            entity.bb$setImmuneUntilTick(Math.max(entity.bb$getImmuneUntilTick(), tick));
        }
    }
}
