package rocks.blackblock.perf.activation_range;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.perf.interfaces.activation_range.HasEntityClusters;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage entity clusters
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class EntityClusterManager implements HasEntityClusters {

    // The world this manager is for
    private final ServerWorld world;

    // The actual entity clusters
    private final List<EntityCluster> entity_clusters = new ArrayList<>(300);

    /**
     * Initialize the manager
     * @since 0.1.0
     */
    public EntityClusterManager(@NotNull ServerWorld world) {
        this.world = world;
    }

    /**
     * Get the entity clusters
     * @since 0.1.0
     */
    @Override
    public List<EntityCluster> bb$getEntityClusters() {
        return this.entity_clusters;
    }

    /**
     * Update the entity groups for the given world
     * @since 0.1.0
     */
    public void recreateEntityGroups() {

        long start = System.currentTimeMillis();

        // Remove all the old clusters
        this.entity_clusters.clear();

        for (Entity entity : this.world.iterateEntities()) {

            if (!(entity instanceof LivingEntity living)) {
                continue;
            }

            if (entity instanceof PlayerEntity) {
                continue;
            }

            this.createInitialGroups(entity);
        }

        // Second pass: merge groups
        this.mergeGroups();

        // Create super clusters
        this.createSuperClusters();
    }

    /**
     * The first pass of grouping entities
     * @since 0.1.0
     */
    private void createInitialGroups(Entity entity) {

        Vec3d pos = entity.getPos();

        // In the first pass, we only add entities to existing groups
        // when they are very close
        for (EntityCluster group : this.entity_clusters) {
            if (group.getSmallMergeBox().contains(pos)) {
                group.addEntity(entity);
                return;
            }
        }

        EntityCluster new_group = new EntityCluster(this.world, entity, this.entity_clusters.size());
        this.entity_clusters.add(new_group);
    }

    /**
     * Second pass: merge groups
     * @since 0.1.0
     */
    private void mergeGroups() {

        int iterations = 0;

        // Create a spatial hash grid
        SpatialHashGrid grid = new SpatialHashGrid(20.0); // Cell size of 20 blocks

        // Add all clusters to the grid
        for (EntityCluster cluster : this.entity_clusters) {
            grid.addCluster(cluster);
        }

        List<EntityCluster> merged_clusters = new ArrayList<>();

        int merge_count = 0;

        while (!grid.isEmpty()) {
            iterations++;

            // Retrieve and remove the next cluster from the grid
            EntityCluster cluster = grid.removeNextCluster();

            // Get all the nearby groups
            List<EntityCluster> nearby_clusters = grid.getNearbyGroups(cluster);

            if (nearby_clusters.isEmpty()) {
                merged_clusters.add(cluster);
                continue;
            }

            boolean merged = false;
            for (EntityCluster neighbour : nearby_clusters) {

                EntityCluster merged_cluster = tryMergeGroups(cluster, neighbour);

                if (merged_cluster != null) {
                    merged = true;
                    merge_count++;

                    grid.addCluster(merged_cluster);
                    cluster = merged_cluster;

                    // Ensure neighbour is removed if merged successfully
                    grid.removeCluster(neighbour);

                    break;
                }
            }

            if (!merged) {
                merged_clusters.add(cluster);
            }
        }

        this.entity_clusters.clear();
        this.entity_clusters.addAll(merged_clusters);

        for (int i = 0; i < this.entity_clusters.size(); i++) {
            this.entity_clusters.get(i).setId(i);
        }
    }

    /**
     * Should the given entity groups merge?
     * @since 0.1.0
     */
    private EntityCluster tryMergeGroups(EntityCluster group1, EntityCluster group2) {

        if (group1 == group2) {
            return null;
        }

        // If both groups don't intersect, they can't merge
        if (!group1.getLargeMergeBox().intersects(group2.getLargeMergeBox())) {
            return null;
        }

        EntityCluster merged_cluster = new EntityCluster(group1, group2);

        // If the groups inner box merges, they should merge
        if (group1.getBoundingBox().intersects(group2.getBoundingBox())) {
            return merged_cluster;
        }

        double allowance = 0.5;

        if (group1.getCenter().y == group2.getCenter().y) {
            allowance += 0.3;
        }

        double solid_blocks_between = group1.calculateSolidBlockDensityBetween(group2.getBoundingBox());

        // The more blocks between, the more the allowance decreases
        if (solid_blocks_between > 0) {
            allowance *= (1 - solid_blocks_between);
        }

        if (group1.getSize() > 3 || group2.getSize() > 3) {
            double distance_1 = group1.getAverageSquaredDistanceToCenter();
            double distance_2 = group2.getAverageSquaredDistanceToCenter();
            double distance_allowance = 1.3 + allowance;

            if (group2.getCenter().squaredDistanceTo(group1.getCenter()) > (distance_1 * distance_allowance)) {
                return null;
            }

            if (group1.getCenter().squaredDistanceTo(group2.getCenter()) > (distance_2 * distance_allowance)) {
                return null;
            }
        }

        double score_1 = group1.getScore();
        double score_2 = group2.getScore();
        double sum_score = score_1 + score_2;

        double merged_score = merged_cluster.getScore();

        if (merged_score < sum_score * (1 - allowance)) {
            return null;
        }

        return merged_cluster;
    }

    /**
     * Create super clusters
     * @since 0.1.0
     */
    private void createSuperClusters() {

        int iterations = 0;

        // Create a spatial hash grid with a larger cell size for super clusters
        SpatialHashGrid grid = new SpatialHashGrid(40.0); // Larger cell size for super clusters

        // Add all clusters to the grid
        for (EntityCluster cluster : this.entity_clusters) {
            grid.addCluster(cluster);
        }

        int mergeCount = 0;

        while (!grid.isEmpty()) {
            iterations++;

            // Retrieve and remove the next cluster from the grid
            EntityCluster cluster = grid.removeNextCluster();

            // Get all the nearby groups
            List<EntityCluster> nearbyClusters = grid.getNearbyGroups(cluster);

            boolean merged = false;
            for (EntityCluster neighbour : nearbyClusters) {
                EntityCluster mergedCluster = tryMergeForSuperClusters(cluster, neighbour);

                if (mergedCluster != null) {
                    merged = true;
                    mergeCount++;

                    grid.removeCluster(neighbour);
                    grid.addCluster(mergedCluster);
                    cluster = mergedCluster;
                    break;
                }
            }
        }
    }

    /**
     * Try to merge the given groups for super clusters
     * @since 0.1.0
     */
    private EntityCluster tryMergeForSuperClusters(EntityCluster group1, EntityCluster group2) {

        Box box_1 = group1.getBoundingBox().expand(3);
        Box box_2 = group2.getBoundingBox().expand(3);

        // If both groups don't intersect, they can't merge
        if (!box_1.intersects(box_2)) {
            return null;
        }

        EntityCluster merged_cluster = new EntityCluster(group1, group2);
        merged_cluster.setSuperCluster();
        merged_cluster.addChildCluster(group1);
        merged_cluster.addChildCluster(group2);

        return merged_cluster;
    }
}
