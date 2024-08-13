package rocks.blackblock.perf.mixin.sync_load;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rocks.blackblock.bib.util.BibChunk;

/**
 * Don't load chunks for bee hives that are too far away
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(BeeEntity.class)
public abstract class BeeEntityMixin extends AnimalEntity {

    @Shadow
    private @Nullable BlockPos hivePos;

    protected BeeEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyExpressionValue(
            method = "isHiveValid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/passive/BeeEntity;isTooFar(Lnet/minecraft/util/math/BlockPos;)Z"
            )
    )
    private boolean bb$onIsHiveValid(boolean is_hive_valid) {
        return is_hive_valid || BibChunk.isChunkLoaded(this.getWorld(), this.hivePos);
    }
}
