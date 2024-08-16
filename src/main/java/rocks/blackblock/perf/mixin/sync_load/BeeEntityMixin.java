package rocks.blackblock.perf.mixin.sync_load;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.bib.util.BibChunk;

/**
 * Don't load chunks for bee hives that are too far away,
 * and also limit the distance to the hive
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(BeeEntity.class)
public abstract class BeeEntityMixin extends AnimalEntity {

    @Shadow
    @Nullable
    BlockPos hivePos;

    protected BeeEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(
        method = "isHiveNearFire",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getBlockEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/entity/BlockEntity;"
        ),
        cancellable = true
    )
    private void bb$onIsHiveNearFire(CallbackInfoReturnable<Boolean> cir) {
        if (this.bb$isTooFarOrUnloaded(this.hivePos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "doesHiveHaveSpace",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bb$onDoesHiveHaveSpace(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.bb$isTooFarOrUnloaded(pos)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
        method = "isHiveValid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/BeeEntity;isTooFar(Lnet/minecraft/util/math/BlockPos;)Z"
        )
    )
    private boolean bb$onIsHiveValid(BeeEntity instance, BlockPos pos) {
        return this.bb$isTooFarOrUnloaded(pos);
    }

    /**
     * Make sure the other position is allowed
     */
    @Unique
    private boolean bb$isTooFarOrUnloaded(BlockPos distant_pos) {
        if (distant_pos == null) {
            return false;
        }

        BlockPos our_pos = this.getBlockPos();

        if (our_pos == null) {
            return false;
        }

        if (!our_pos.isWithinDistance(distant_pos, 32)) {
            return true;
        }

        return !BibChunk.isChunkLoaded(this.getWorld(), distant_pos);
    }
}
