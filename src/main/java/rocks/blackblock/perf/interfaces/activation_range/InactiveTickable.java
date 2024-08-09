package rocks.blackblock.perf.interfaces.activation_range;

/**
 * Makes something that is tickable, but inactive,
 * receive an "inactive" tick
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface InactiveTickable {

    /**
     * Special logic per tick when inactive
     * @since    0.1.0
     */
    default void bb$inactiveTick() {

    }
}
