package rocks.blackblock.perf.interfaces.activation_range;

import rocks.blackblock.perf.activation_range.ActivationRange;

/**
 * Makes an entity deactivatable
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface DeactivatableEntity {

    /**
     * Get the entity's activation range setting
     * @since 0.1.0
     */
    ActivationRange bb$getActivationRange();

    /**
     * See if the entity is excluded from dynamic activation range
     * @since 0.1.0
     */
    boolean bb$isExcludedFromDynamicActivationRange();

    /**
     * Get the tick until which the entity should remain active
     * @since 0.1.0
     */
    int bb$getActivatedUntilTick();

    /**
     * Set the tick until which the entity should remain active
     * @since 0.1.0
     */
    void bb$setActivatedUntilTick(int tick);

    /**
     * Get the tick until which the entity should be immune
     * @since 0.1.0
     */
    int bb$getImmuneUntilTick();

    /**
     * Set the tick until which the entity should be immune
     * @since 0.1.0
     */
    void bb$setImmuneUntilTick(int tick);

    /**
     * Is this entity considered to be inactive?
     * @since 0.1.0
     */
    boolean bb$isInactive();

    /**
     * Set the inactivity of this entity
     * @since 0.1.0
     */
    void bb$setInactive(boolean inactive);

    /**
     * Increment the potential tick count
     * @since 0.1.0
     */
    void bb$incrementPotentialTickCount();

    /**
     * Get the amount of ticks that this entity would have
     * originally been ticked for.
     * @since 0.1.0
     */
    default int bb$getPotentialTickCount() {
        return 0;
    }
}
