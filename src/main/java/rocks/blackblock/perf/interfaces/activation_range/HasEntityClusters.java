package rocks.blackblock.perf.interfaces.activation_range;

import rocks.blackblock.perf.activation_range.EntityCluster;

import java.util.List;

/**
 * Let something have entity clusters,
 * probably a World class
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public interface HasEntityClusters {
    default List<EntityCluster> bb$getEntityClusters() {
        return null;
    }
}
