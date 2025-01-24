package rocks.blackblock.perf.mixin.threading;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(RedstoneWireBlock.class)
public abstract class RedStoneWireBlockMixin {

    @Shadow
    @Final
    public static IntProperty POWER;

    @Shadow
    @Final
    public static Map<Direction, EnumProperty<WireConnection>> DIRECTION_TO_WIRE_CONNECTION_PROPERTY;

    /**
     * The `wiresGivePower` property is not thread-safe since it's a global flag.
     * To ensure no interference between threads the field is replaced with this thread local one.
     */
    @Unique
    private final ThreadLocal<Boolean> bb$wiresGivePowerSafe = ThreadLocal.withInitial(() -> true);

    @Shadow
    protected abstract BlockState getPlacementState(BlockView world, BlockState state, BlockPos pos);

    @Inject(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/RedstoneController;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/world/block/WireOrientation;Z)V",
            shift = At.Shift.BEFORE
        )
    )
    private void getReceivedRedstonePowerBefore(World world, BlockPos pos, BlockState state, WireOrientation orientation, boolean blockAdded, CallbackInfo ci) {
        this.bb$wiresGivePowerSafe.set(false);
    }

    @Inject(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/RedstoneController;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/world/block/WireOrientation;Z)V",
            shift = At.Shift.AFTER
        )
    )
    private void getReceivedRedstonePowerAfter(World world, BlockPos pos, BlockState state, WireOrientation orientation, boolean blockAdded, CallbackInfo ci) {
        this.bb$wiresGivePowerSafe.set(true);
    }

    /**
     * @author DimensionalThreading (WearBlackAllDay)
     * @reason Made redstone thread-safe, please inject in the caller.
     */
    @Overwrite
    public boolean emitsRedstonePower(BlockState state) {
        return this.bb$wiresGivePowerSafe.get();
    }

    /**
     * @author DimensionalThreading (WearBlackAllDay)
     * @reason Made redstone thread-safe, please inject in the caller.
     */
    @Overwrite
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return !this.bb$wiresGivePowerSafe.get() ? 0 : state.getWeakRedstonePower(world, pos, direction);
    }

    /**
     * @author DimensionalThreading (WearBlackAllDay)
     * @reason Made redstone thread-safe, please inject in the caller.
     */
    @Overwrite
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (!this.bb$wiresGivePowerSafe.get() || direction == Direction.DOWN) {
            return 0;
        }

        int i = state.get(POWER);
        if (i == 0) return 0;
        return direction != Direction.UP && !this.getPlacementState(world, state, pos)
                .get(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction.getOpposite())).isConnected() ? 0 : i;
    }

}
