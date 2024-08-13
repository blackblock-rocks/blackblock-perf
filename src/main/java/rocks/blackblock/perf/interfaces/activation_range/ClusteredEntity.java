package rocks.blackblock.perf.interfaces.activation_range;

import rocks.blackblock.perf.activation_range.EntityCluster;

/**
 * Let entities be clustered/grouped with other entities
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface ClusteredEntity {
    EntityCluster bb$getCluster();
    void bb$setCluster(EntityCluster cluster);
}
