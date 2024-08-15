package rocks.blackblock.perf.mixin.deduplicate_fluid_ticks;

import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The {@link FluidState} onRandomTick method is called twice per tick.
 * This mixin prevents the second call.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(value = FluidBlock.class, priority = 900)
public class FluidBlockMixin {

    @Redirect(
        method = "hasRandomTicks",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/fluid/FluidState;hasRandomTicks()Z"
        )
    )
    private boolean bb$cancelDuplicateFluidTicks(FluidState fluidState) {
        return false;
    }
}
