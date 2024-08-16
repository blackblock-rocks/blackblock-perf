package rocks.blackblock.perf.mixin.sign_ticking;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Clear any leftover, invalid editors upon getting it.
 *
 * Credit to PaperMC patch #0974
 *
 * @since    0.1.0
 */
@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity {

    @Shadow
    @Nullable
    private UUID editor;

    @Shadow
    protected abstract void tryClearInvalidEditor(SignBlockEntity blockEntity, World world, UUID uuid);

    public SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "getEditor", at = @At("HEAD"))
    private void bb$onGetEditor(CallbackInfoReturnable<UUID> cir) {
        if (this.hasWorld() && this.editor != null) {
            this.tryClearInvalidEditor((SignBlockEntity)(Object)this, this.getWorld(), this.editor);
        }
    }
}
