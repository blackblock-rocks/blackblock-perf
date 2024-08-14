package rocks.blackblock.perf.spawn;

import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.entity.SpawnGroup;

/**
 * Utility class for the DensityCapMixin.
 * We have to put this here because inner classes are not allowed in mixins.
 *
 * @author   ishland
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class DensityCapDelegate {

    public static Object2IntMap<SpawnGroup> delegateSpawnGroupDensities(int[] spawnGroupDensities) {
        return new AbstractObject2IntMap<>() {
            @Override
            public int size() {
                return spawnGroupDensities.length;
            }

            @Override
            public ObjectSet<Entry<SpawnGroup>> object2IntEntrySet() {
                return new AbstractObjectSet<>() {
                    @Override
                    public ObjectIterator<Entry<SpawnGroup>> iterator() {
                        return new AbstractObjectIterator<>() {
                            private int index = 0;

                            @Override
                            public boolean hasNext() {
                                return index < spawnGroupDensities.length;
                            }

                            @Override
                            public it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<SpawnGroup> next() {
                                final int index = this.index;
                                this.index++;
                                return new AbstractObject2IntMap.BasicEntry<>(
                                        SpawnGroup.values()[index], spawnGroupDensities[index]);
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return spawnGroupDensities.length;
                    }
                };
            }

            @Override
            public int getInt(Object key) {
                return spawnGroupDensities[((SpawnGroup) key).ordinal()];
            }
        };
    }
}
