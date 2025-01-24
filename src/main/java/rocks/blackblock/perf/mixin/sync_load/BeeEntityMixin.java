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

    protected BeeEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(
        method = "doesHiveHaveSpace",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bb$onDoesHiveHaveSpace(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.bb$isChunkLoaded(pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "isWithinDistance",
        at = @At(
            value = "RETURN"
        ),
        cancellable = true
    )
    private void bb$onIsWithinDistance(BlockPos pos, int distance, CallbackInfoReturnable<Boolean> cir) {
        boolean result = cir.getReturnValue();

        if (!result) {
            return;
        }

        result = this.bb$isChunkLoaded(pos);

        if (!result) {
            cir.setReturnValue(result);
        }
    }

    /**
     * Make sure the other position is allowed
     */
    @Unique
    private boolean bb$isChunkLoaded(BlockPos distant_pos) {
        if (distant_pos == null) {
            return false;
        }

        BlockPos our_pos = this.getBlockPos();

        if (our_pos == null) {
            return false;
        }

        return BibChunk.isChunkLoaded(this.getWorld(), distant_pos);
    }
}
