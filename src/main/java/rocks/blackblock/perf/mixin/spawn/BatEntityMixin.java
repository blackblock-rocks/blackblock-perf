package rocks.blackblock.perf.mixin.spawn;

import com.moulberry.mixinconstraints.annotations.IfModAbsent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import rocks.blackblock.bib.util.BibTime;

/**
 * Get the light level 50% less often when trying to spawn bats
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@IfModAbsent(value = "nocturnal-bats")
@Mixin(BatEntity.class)
public class BatEntityMixin {

    /**
     * @author Jelle De Loecker <jelle@elevenways.be>
     * @reason Skip getting the light level
     */
    @Overwrite
    public static boolean canSpawn(EntityType<BatEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {

        if (pos.getY() >= world.getSeaLevel()) {
            return false;
        }

        int spawn_light_threshold = 4;

        if (BibTime.IS_AROUND_HALLOWEEN) {
            spawn_light_threshold = 7;
        } else if (random.nextBoolean()) {
            return false;
        }

        if (world.getLightLevel(pos) > random.nextInt(spawn_light_threshold)) {
            return false;
        }

        return MobEntity.canMobSpawn(type, world, spawnReason, pos, random);
    }
}
