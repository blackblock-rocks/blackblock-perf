package rocks.blackblock.perf.activation_range;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class SpatialHashGrid {
    private final double cellSize;
    private final Long2ObjectOpenHashMap<List<EntityCluster>> grid = new Long2ObjectOpenHashMap<>();
    private final Deque<EntityCluster> clusterQueue = new ArrayDeque<>();
    private final long[] neighborOffsets = new long[27];

    public SpatialHashGrid(double cellSize) {
        this.cellSize = cellSize;
        initNeighborOffsets();
    }

    private void initNeighborOffsets() {
        int index = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    neighborOffsets[index++] = x + (((long) y) << 20) + (((long) z) << 40);
                }
            }
        }
    }

    public void addCluster(EntityCluster cluster) {
        long hash = hashPosition(cluster.getCenter());
        grid.computeIfAbsent(hash, k -> new ArrayList<>()).add(cluster);
        clusterQueue.offer(cluster);
    }

    public void removeCluster(EntityCluster cluster) {
        long hash = hashPosition(cluster.getCenter());
        List<EntityCluster> cell = grid.get(hash);
        if (cell != null) {
            cell.remove(cluster);
            if (cell.isEmpty()) {
                grid.remove(hash);
            }
        }
        clusterQueue.remove(cluster);
    }

    public List<EntityCluster> getNearbyGroups(EntityCluster cluster) {
        List<EntityCluster> nearby = new ArrayList<>();
        Vec3d center = cluster.getCenter();
        long hash = hashPosition(center);

        for (long offset : neighborOffsets) {
            long neighborHash = hash + offset;
            List<EntityCluster> cell = grid.get(neighborHash);
            if (cell != null) {
                nearby.addAll(cell);
            }
        }

        nearby.remove(cluster);
        return nearby;
    }

    public int getSize() {
        return clusterQueue.size();
    }

    public boolean isEmpty() {
        return clusterQueue.isEmpty();
    }

    public EntityCluster removeNextCluster() {
        var cluster = clusterQueue.poll();

        if (cluster != null) {
            this.removeCluster(cluster);
        }

        return cluster;
    }

    private long hashPosition(Vec3d position) {
        int x = (int) Math.floor(position.x / cellSize);
        int y = (int) Math.floor(position.y / cellSize);
        int z = (int) Math.floor(position.z / cellSize);
        return x + (((long) y) << 20) + (((long) z) << 40);
    }
}