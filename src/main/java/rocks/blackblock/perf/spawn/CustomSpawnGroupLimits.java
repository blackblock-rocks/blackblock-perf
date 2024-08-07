package rocks.blackblock.perf.spawn;

import net.minecraft.world.World;

/**
 * Dynamic mob category limits
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface CustomSpawnGroupLimits {

    /**
     * How much time to wait between spawns
     * @since 0.1.0
     */
    int bb$getSpawnInterval(World world);

    /**
     * Set a new spawn interval
     * @since 0.1.0
     */
    void bb$setSpawnInterval(World world, int interval);

    /**
     * Get the original, unmodified capacity
     * @since 0.1.0
     */
    int bb$getOriginalCapacity();

    /**
     * Set the new base capacity
     * @since 0.1.0
     */
    void bb$setBaseCapacity(World world, int capacity);

    /**
     * Get the base capacity
     * @since 0.1.0
     */
    int bb$getBaseCapacity(World world);

    /**
     * Set the capacity modifier (as a percentage, 0.0-1.0)
     * @since 0.1.0
     */
    void bb$setCapacityModifier(World world, double modifier);

    /**
     * Get the capacity
     * @since 0.1.0
     */
    int bb$getCapacity(World world);
}
