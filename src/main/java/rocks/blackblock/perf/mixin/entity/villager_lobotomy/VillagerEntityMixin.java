package rocks.blackblock.perf.mixin.entity.villager_lobotomy;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import rocks.blackblock.bib.util.BibChunk;

/**
 * Lobotomize villagers stuck in small spaces
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {

    @Unique
    private static int TICK_INTERVAL_WHEN_LOBOTOMIZED = 10;

    @Unique
    private boolean bb$lobotomized = false;

    @Unique
    private int bb$not_lobotomized_count = 0;

    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    @WrapWithCondition(
            method = "mobTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/brain/Brain;tick(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;)V"
            )
    )
    private boolean bb$shouldTickBrain(Brain<VillagerEntity> instance, ServerWorld world, LivingEntity entity) {
        return !this.bb$isLobotomized() || this.age % TICK_INTERVAL_WHEN_LOBOTOMIZED == 0 || this.isInFluid();
    }

    /**
     * Is this villager lobotomized?
     * @since    0.1.0
     */
    @Unique
    private boolean bb$isLobotomized() {

        // Check half as often if not lobotomized for the last 3+ consecutive checks
        int check_count = this.bb$not_lobotomized_count > 3 ? 600 : 300;

        if (this.age % check_count == 0) {

        }

        return this.bb$lobotomized;
    }

    /**
     * Check if the villager is able to move anywhere
     * @since    0.1.0
     */
    @Unique
    private boolean bb$canTravel() {
        // Offset Y for short blocks like dirt_path/farmland
        BlockPos center = BlockPos.ofFloored(this.getX(), this.getY() + 0.0625D, this.getZ());

        Chunk chunk = BibChunk.getChunkNow(this.getWorld(), center);

        if (chunk == null) {
            return false;
        }

        BlockPos.Mutable mutable = center.mutableCopy();
        boolean canJump = !this.bb$hasCollisionAt(chunk, mutable.move(Direction.UP, 2));

        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (this.bb$canTravelTo(mutable.set(center, direction), canJump)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Can this villager travel to the given position?
     * @since    0.1.0
     */
    @Unique
    private boolean bb$canTravelTo(BlockPos.Mutable mutable, boolean canJump) {

        Chunk chunk = BibChunk.getChunkNow(this.getWorld(), mutable);

        if (chunk == null) {
            return false;
        }

        Block bottom = chunk.getBlockState(mutable).getBlock();
        if (bottom instanceof BedBlock) {
            // Allows iron farms to function normally
            return true;
        }

        if (this.bb$hasCollisionAt(chunk, mutable.move(Direction.UP))) {
            // Early return if the top block has collision.
            return false;
        }

        // The villager can only jump if:
        // - There is no collision above the villager
        // - There is no collision above the top block
        // - The bottom block is short enough to jump on
        boolean isTallBlock = bottom instanceof FenceBlock || bottom instanceof FenceGateBlock || bottom instanceof WallBlock;
        return !bottom.collidable || (canJump && !isTallBlock && !this.bb$hasCollisionAt(chunk, mutable.move(Direction.UP)));
    }

    /**
     * Is the given block at the given position collidable?
     * @since    0.1.0
     */
    @Unique
    private boolean bb$hasCollisionAt(Chunk chunk, BlockPos pos) {
        return chunk.getBlockState(pos).getBlock().collidable;
    }
}
