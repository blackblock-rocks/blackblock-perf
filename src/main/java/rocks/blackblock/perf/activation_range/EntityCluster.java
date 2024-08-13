package rocks.blackblock.perf.activation_range;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import rocks.blackblock.bib.util.BibLog;

import java.util.ArrayList;
import java.util.List;

/**
 * A cluster of entities close together
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class EntityCluster implements BibLog.Argable {

    public static final double INITIAL_GROUP_RANGE = 2.0;
    public static final double GROUP_MERGE_RANGE = 0.5;
    public static final int MIN_GROUP_SIZE_FOR_MERGE = 1;
    public static final float DENSITY_MERGE_THRESHOLD = 0.7f;

    private ServerWorld world;
    private List<Entity> entities;
    private Box bounding_box;
    private int tickCounter = 0;
    private int id;
    private Float cached_density = null;

    // The amount of non-solid blocks in the cluster
    private Integer non_solid_blocks = null;

    // The score of this cluster
    private Double score = null;

    // The squared distance to the center of the cluster
    private Double center_distance_squared = null;

    // The small merge box is a tiny bit larger than the bounding box
    private Box small_merge_box;

    // The large merge box is larger than the small merge box
    private Box large_merge_box;

    // The super cluster this is a part of
    private EntityCluster super_cluster = null;

    // The child clusters
    private List<EntityCluster> child_clusters = new ArrayList<>();

    // Is this a super cluster?
    private boolean is_super_cluster = false;

    /**
     * Calculate the box volume
     * @since 0.1.0
     */
    public static double calculateBoxVolume(Box box) {
        return box.getLengthX() * box.getLengthY() * box.getLengthZ();
    }

    /**
     * Create a new cluster with the given entity
     * @since 0.1.0
     */
    public EntityCluster(ServerWorld world, Entity initialEntity, int id) {
        this.world = world;
        this.entities = new ArrayList<>();
        this.id = id;
        this.addEntity(initialEntity);
    }

    /**
     * Create a new cluster from two existing clusters
     * @since 0.1.0
     */
    public EntityCluster(EntityCluster group1, EntityCluster group2) {
        this.world = group1.world;
        this.entities = new ArrayList<>(group1.entities.size() + group2.entities.size());

        for (Entity entity : group1.entities) {
            this.addEntitySilently(entity);
        }

        for (Entity entity : group2.entities) {
            this.addEntitySilently(entity);
        }

        this.recalculateBoundingBox();
        this.id = -1;
    }

    /**
     * Is this a super cluster?
     * @since 0.1.0
     */
    public boolean isSuperCluster() {
        return this.is_super_cluster;
    }

    /**
     * Set this cluster as a super cluster
     * @since 0.1.0
     */
    public void setSuperCluster() {
        this.is_super_cluster = true;
    }

    /**
     * Add the given cluster as a child cluster
     * @since 0.1.0
     */
    public void addChildCluster(EntityCluster cluster) {

        if (cluster.isSuperCluster()) {
            for (EntityCluster child_cluster : cluster.child_clusters) {
                this.addChildCluster(child_cluster);
            }
            return;
        }

        this.child_clusters.add(cluster);
        cluster.setSuperCluster(this);
    }

    /**
     * Set the identifier of this cluster
     * @since 0.1.0
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Set the super cluster this is a part of
     * @since 0.1.0
     */
    public void setSuperCluster(EntityCluster super_cluster) {
        this.super_cluster = super_cluster;
    }

    /**
     * Get the super cluster this is a part of
     * @since 0.1.0
     */
    public EntityCluster getSuperCluster() {
        return this.super_cluster;
    }

    /**
     * Add an entity to this cluster
     * @since 0.1.0
     */
    public void addEntity(Entity entity) {
        this.addEntitySilently(entity);
        this.addEntityToBoundingBox(entity);
    }

    /**
     * Add an entity to this cluster silently
     * @since 0.1.0
     */
    private void addEntitySilently(Entity entity) {
        this.entities.add(entity);
        entity.bb$setCluster(this);
    }

    /**
     * Add the given entity to the bounding box
     * @since 0.1.0
     */
    public void addEntityToBoundingBox(Entity entity) {

        if (this.bounding_box == null) {
            this.bounding_box = entity.getBoundingBox();
        } else {
            this.bounding_box = this.bounding_box.union(entity.getBoundingBox());
        }

        this.recalculateMergeBoxes();
    }

    /**
     * Recalculate the bounding box from scratch
     * @since 0.1.0
     */
    private void recalculateBoundingBox() {
        if (entities.isEmpty()) {
            bounding_box = Box.of(Vec3d.ZERO, 0, 0, 0);
            small_merge_box = bounding_box;
            large_merge_box = bounding_box;
        } else {
            bounding_box = entities.get(0).getBoundingBox();
            for (int i = 1; i < entities.size(); i++) {
                bounding_box = bounding_box.union(entities.get(i).getBoundingBox());
            }

            this.recalculateMergeBoxes();
        }
    }

    /**
     * Recalculate the merge boxes
     * @since 0.1.0
     */
    public void recalculateMergeBoxes() {

        Box old_box = this.small_merge_box;

        this.small_merge_box = this.bounding_box.expand(0.3, 0.1, 0.3);
        this.large_merge_box = this.bounding_box.expand(1.5, 0.0, 1.5);

        if (this.non_solid_blocks != null) {
            boolean clear_solid_block_cache = false;

            if (old_box == null) {
                clear_solid_block_cache = true;
            } else {
                double old_volume = calculateBoxVolume(old_box);
                double new_volume = calculateBoxVolume(this.small_merge_box);
                double delta_volume = Math.abs(new_volume - old_volume);

                if (delta_volume > 1) {
                    clear_solid_block_cache = true;
                }
            }

            if (clear_solid_block_cache) {
                this.non_solid_blocks = null;
            }
        }
    }

    /**
     * Calculate the solid blocks from this center
     * to the other position
     * @since 0.1.0
     */
    public int calculateSolidBlocks(Box other) {
        int solid_blocks = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // Get the union of the two boxes
        Box union = this.bounding_box.union(other);

        for (int x = (int) Math.floor(union.minX); x < Math.ceil(union.maxX); x++) {
            for (int y = (int) Math.floor(union.minY); y < Math.ceil(union.maxY); y++) {
                for (int z = (int) Math.floor(union.minZ); z < Math.ceil(union.maxZ); z++) {
                    mutable.set(x, y, z);
                    BlockState blockState = world.getBlockState(mutable);
                    if (blockState.isSolid()) {
                        solid_blocks++;
                    }
                }
            }
        }

        return solid_blocks;
    }

    public double calculateSolidBlockDensityBetween(Box other) {

        // Get the union of the two boxes
        Box union = this.bounding_box.union(other);

        int solid_blocks = this.calculateSolidBlocks(other);
        double volume = calculateBoxVolume(union);
        double solid_blocks_per_volume = solid_blocks / volume;

        return solid_blocks_per_volume;
    }

    /**
     * Return the "small" merge box:
     * this is used for initially populating the clusters
     * @since 0.1.0
     */
    public float getDensity() {

        if (this.cached_density != null) {
            return this.cached_density;
        }

        int size = entities.size();
        if (size <= 1) {
            this.cached_density = 0f;
            return 0;
        }

        int non_solid_blocks = this.countNonSolidBlocks();

        // This assumes all entities occupy a 1x2 space,
        // which is not true, but close enough
        double entity_volume = size * 2;

        // Calculate density as a ratio of entities per non-solid blocks
        this.cached_density = (float) Math.min(entity_volume / non_solid_blocks, 1.0);

        return this.cached_density;
    }

    /**
     * Get all the non-solid blocks in the bounding box
     * @since 0.1.0
     */
    private int countNonSolidBlocks() {

        if (this.non_solid_blocks != null) {
            return this.non_solid_blocks;
        }

        int count = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = (int) Math.floor(bounding_box.minX); x < Math.ceil(bounding_box.maxX); x++) {
            for (int y = (int) Math.floor(bounding_box.minY); y < Math.ceil(bounding_box.maxY); y++) {
                for (int z = (int) Math.floor(bounding_box.minZ); z < Math.ceil(bounding_box.maxZ); z++) {
                    mutable.set(x, y, z);
                    BlockState blockState = world.getBlockState(mutable);
                    if (!blockState.isSolid()) {
                        count++;
                    }
                }
            }
        }

        this.non_solid_blocks = Math.max(count, 1); // Ensure we don't divide by zero

        return this.non_solid_blocks;
    }

    /**
     * Get the score of this cluster
     * @since 0.1.0
     */
    public double getScore() {
        if (this.score == null) {
            int size = this.getSize();

            if (size <= 1) {
                this.score = 0.1;
            } else {
                this.score = this.getSize() / (1 + Math.sqrt(this.calculateAverageSquaredDistanceToCenter()));
            }
        }

        return this.score;
    }

    /**
     * Get the squared distance to the center of the cluster
     * @since 0.1.0
     */
    public double getAverageSquaredDistanceToCenter() {

        if (this.center_distance_squared == null) {
            this.center_distance_squared = this.calculateAverageSquaredDistanceToCenter();
        }

        return this.center_distance_squared;
    }

    /**
     * Calculate the average distance to the center of the cluster
     * @since 0.1.0
     */
    private double calculateAverageSquaredDistanceToCenter() {

        int size = this.getSize();

        if (size <= 1) {
            return 0;
        }

        double average_distance = 0;
        Vec3d center = this.getBoundingBox().getCenter();

        for (Entity entity : entities) {
            average_distance += entity.getPos().squaredDistanceTo(center);
        }

        return average_distance / size;
    }

    /**
     * Get the cluster center
     * @since 0.1.0
     */
    public Vec3d getCenter() {
        return this.getBoundingBox().getCenter();
    }

    /**
     * Get the bounding box of this cluster
     * @since 0.1.0
     */
    public Box getBoundingBox() {
        return bounding_box;
    }

    /**
     * Return the "small" merge box:
     * this is used for initially populating the clusters
     * @since 0.1.0
     */
    public Box getSmallMergeBox() {
        return this.small_merge_box;
    }

    /**
     * Return the "large" merge box:
     * this is used for merging clusters
     * @since 0.1.0
     */
    public Box getLargeMergeBox() {
        return this.large_merge_box;
    }

    /**
     * Return the number of entities in this cluster
     * @since 0.1.0
     */
    public int getSize() {
        return entities.size();
    }

    /**
     * Return the BibLog.Arg representation of this cluster
     * @since 0.1.0
     */
    @Override
    public BibLog.Arg toBBLogArg() {
        var result = BibLog.createArg("EntityGroup");
        result.add("id", id);
        result.add("size", getSize());
        result.add("density", getDensity());
        result.add("bounding_box", this.bounding_box);
        result.add("average_distance_to_center", this.getAverageSquaredDistanceToCenter());
        result.add("score", this.getScore());
        return result;
    }

    /**
     * Return the string representation of this cluster
     * @since 0.1.0
     */
    @Override
    public String toString() {
        return this.toBBLogArg().toString();
    }
}
