package rocks.blackblock.perf.spawn;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public interface CheckBelowCapPerWorld {
    boolean bb$isBelowCap(World world, SpawnGroup group, ChunkPos chunk_pos);
}
