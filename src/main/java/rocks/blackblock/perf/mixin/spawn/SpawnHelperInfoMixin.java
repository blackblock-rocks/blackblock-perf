package rocks.blackblock.perf.mixin.spawn;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnDensityCapper;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import rocks.blackblock.perf.spawn.CheckBelowCapPerWorld;

/**
 * Add the bb$isBelowCap method to the SpawnHelper.Info class
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(SpawnHelper.Info.class)
public class SpawnHelperInfoMixin implements CheckBelowCapPerWorld {

    @Unique
    private static final int CHUNK_AREA = (int)Math.pow(17.0, 2.0);

    @Shadow @Final private int spawningChunkCount;

    @Shadow @Final private Object2IntOpenHashMap<SpawnGroup> groupToCount;

    @Shadow @Final private SpawnDensityCapper densityCapper;

    @Override
    public boolean bb$isBelowCap(World world, SpawnGroup group, ChunkPos chunk_pos) {
        int i = group.bb$getCapacity(world) * this.spawningChunkCount / CHUNK_AREA;
        return this.groupToCount.getInt(group) >= i ? false : this.densityCapper.canSpawn(group, chunk_pos);
    }
}
