package rocks.blackblock.perf.distance;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import rocks.blackblock.bib.util.BibPos;
import rocks.blackblock.perf.util.SimpleObjectPool;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Efficient 2D spatial data structure for managing objects (typically players) in a grid-based area.
 * Optimized for fast lookups and updates of object positions and view distances.
 * Supports custom listeners for object addition and removal events.
 *
 * Based on the work in VMP by ishland
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 *
 * @param <T> The type of objects being managed in the area map.
 */
public class AreaMap<T> {

    private static final boolean DEBUG = Boolean.getBoolean("vmp.debugAreaMap");

    private static final Object[] EMPTY = new Object[0];
    private static final RawObjectLinkedOpenIdentityHashSet<?> EMPTY_SET = new RawObjectLinkedOpenIdentityHashSet<>();

    private final SimpleObjectPool<RawObjectLinkedOpenIdentityHashSet<T>> pooledHashSets =
            new SimpleObjectPool<>(unused -> new RawObjectLinkedOpenIdentityHashSet<>(),
                    ReferenceLinkedOpenHashSet::clear,
                    ts -> {
                        ts.clear();
                        ts.trim(256);
                    },
                    8192
            );
    private final Long2ObjectFunction<RawObjectLinkedOpenIdentityHashSet<T>> allocHashSet = unused -> pooledHashSets.alloc();
    private final Long2ObjectOpenHashMap<RawObjectLinkedOpenIdentityHashSet<T>> map = new Long2ObjectOpenHashMap<>();
    private final Reference2IntOpenHashMap<T> viewDistances = new Reference2IntOpenHashMap<>();
    private final Reference2LongOpenHashMap<T> lastCenters = new Reference2LongOpenHashMap<>();

    private Listener<T> addListener = null;
    private Listener<T> removeListener = null;
    private final boolean sortListenerCalls;

    public AreaMap() {
        this(null, null, false);
    }

    public AreaMap(Listener<T> addListener, Listener<T> removeListener, boolean sortListenerCalls) {
        this.addListener = addListener;
        this.removeListener = removeListener;
        this.sortListenerCalls = sortListenerCalls;
    }

    public Set<T> getAllObjects() {

        Set<T> result = new HashSet<>();

        for (RawObjectLinkedOpenIdentityHashSet<T> set : map.values()) {
            result.addAll(set);
        }
        return result;
    }

    public Set<T> getObjectsInRange(long coordinateKey) {
        final RawObjectLinkedOpenIdentityHashSet<T> set = map.get(coordinateKey);
        return set != null ? set : (Set<T>) EMPTY_SET;
    }

    public Object[] getObjectsInRangeArray(long coordinateKey) {
        final RawObjectLinkedOpenIdentityHashSet<T> set = map.get(coordinateKey);
        return set != null ? set.getRawSet() : EMPTY;
    }

    public void add(T object, int x, int z, int rawViewDistance) {
        int viewDistance = rawViewDistance;
        viewDistances.put(object, viewDistance);
        lastCenters.put(object, BibPos.toLong(x, z));

        final Listener<T> addListener = this.addListener;
        if (this.sortListenerCalls && addListener != null) {
            final int length = 2 * viewDistance + 1;
            LongArrayList set = new LongArrayList(length * length);
            for (int xx = x - viewDistance; xx <= x + viewDistance; xx++) {
                for (int zz = z - viewDistance; zz <= z + viewDistance; zz++) {
                    add0(xx, zz, object, false);
                    set.add(BibPos.toLong(xx, zz));
                }
            }
            notifyListenersSorted(object, x, z, addListener, set);
        } else {
            for (int xx = x - viewDistance; xx <= x + viewDistance; xx++) {
                for (int zz = z - viewDistance; zz <= z + viewDistance; zz++) {
                    add0(xx, zz, object, true);
                }
            }
        }

        validate(object, x, z, viewDistance);
    }

    public void remove(T object) {
        if (!viewDistances.containsKey(object)) return;
        final int viewDistance = viewDistances.removeInt(object);
        final long lastCenter = lastCenters.removeLong(object);
        final int x = BibPos.getChunkX(lastCenter);
        final int z = BibPos.getChunkZ(lastCenter);

        final Listener<T> removeListener = this.removeListener;
        if (this.sortListenerCalls && removeListener != null) {
            final int length = 2 * viewDistance + 1;
            LongArrayList set = new LongArrayList(length * length);
            for (int xx = x - viewDistance; xx <= x + viewDistance; xx++) {
                for (int zz = z - viewDistance; zz <= z + viewDistance; zz++) {
                    remove0(xx, zz, object, false);
                    set.add(BibPos.toLong(xx, zz));
                }
            }
            notifyListenersSorted(object, x, z, removeListener, set);
        } else {
            for (int xx = x - viewDistance; xx <= x + viewDistance; xx++) {
                for (int zz = z - viewDistance; zz <= z + viewDistance; zz++) {
                    remove0(xx, zz, object, true);
                }
            }
        }

        validate(object, -1, -1, -1);
    }

    public void update(T object, int x, int z, int rawViewDistance) {
        if (!viewDistances.containsKey(object))
            throw new IllegalArgumentException("Tried to update %s when not in map".formatted(object));
        final int viewDistance = rawViewDistance;
        final int oldViewDistance = viewDistances.replace(object, viewDistance);
        final long oldCenter = lastCenters.replace(object, BibPos.toLong(x, z));
        final int oldX = BibPos.getChunkX(oldCenter);
        final int oldZ = BibPos.getChunkZ(oldCenter);

        updateAdds(object, oldX, oldZ, oldViewDistance, x, z, viewDistance);
        updateRemovals(object, oldX, oldZ, oldViewDistance, x, z, viewDistance);

        validate(object, x, z, viewDistance);
    }

    public int uniqueObjects() {
        return this.lastCenters.size();
    }

    private void updateAdds(T object, int oldX, int oldZ, int oldViewDistance, int newX, int newZ, int newViewDistance) {
        int xLower = oldX - oldViewDistance;
        int xHigher = oldX + oldViewDistance;
        int zLower = oldZ - oldViewDistance;
        int zHigher = oldZ + oldViewDistance;

        final Listener<T> addListener = this.addListener;
        if (this.sortListenerCalls && addListener != null) {
            final int length = 2 * newViewDistance + 1;
            LongArrayList set = new LongArrayList(length * length);
            for (int xx = newX - newViewDistance; xx <= newX + newViewDistance; xx++) {
                for (int zz = newZ - newViewDistance; zz <= newZ + newViewDistance; zz++) {
                    if (!isInRange(xLower, xHigher, zLower, zHigher, xx, zz)) {
                        add0(xx, zz, object, false);
                        set.add(BibPos.toLong(xx, zz));
                    }
                }
            }
            notifyListenersSorted(object, newX, newZ, addListener, set);
        } else {
            for (int xx = newX - newViewDistance; xx <= newX + newViewDistance; xx++) {
                for (int zz = newZ - newViewDistance; zz <= newZ + newViewDistance; zz++) {
                    if (!isInRange(xLower, xHigher, zLower, zHigher, xx, zz)) {
                        add0(xx, zz, object, true);
                    }
                }
            }
        }
    }

    private void updateRemovals(T object, int oldX, int oldZ, int oldViewDistance, int newX, int newZ, int newViewDistance) {
        int xLower = newX - newViewDistance;
        int xHigher = newX + newViewDistance;
        int zLower = newZ - newViewDistance;
        int zHigher = newZ + newViewDistance;

        final Listener<T> removeListener = this.removeListener;
        if (this.sortListenerCalls && removeListener != null) {
            final int length = 2 * oldViewDistance + 1;
            LongArrayList set = new LongArrayList(length * length);
            for (int xx = oldX - oldViewDistance; xx <= oldX + oldViewDistance; xx++) {
                for (int zz = oldZ - oldViewDistance; zz <= oldZ + oldViewDistance; zz++) {
                    if (!isInRange(xLower, xHigher, zLower, zHigher, xx, zz)) {
                        remove0(xx, zz, object, false);
                        set.add(BibPos.toLong(xx, zz));
                    }
                }
            }
            notifyListenersSorted(object, newX, newZ, removeListener, set);
        } else {
            for (int xx = oldX - oldViewDistance; xx <= oldX + oldViewDistance; xx++) {
                for (int zz = oldZ - oldViewDistance; zz <= oldZ + oldViewDistance; zz++) {
                    if (!isInRange(xLower, xHigher, zLower, zHigher, xx, zz)) {
                        remove0(xx, zz, object, true);
                    }
                }
            }
        }
    }

    private void add0(int xx, int zz, T object, boolean notifyListeners) {
        final RawObjectLinkedOpenIdentityHashSet<T> set = map.computeIfAbsent(BibPos.toLong(xx, zz), allocHashSet);
        set.add(object);
        if (notifyListeners && this.addListener != null) this.addListener.accept(object, xx, zz);
    }

    private void remove0(int xx, int zz, T object, boolean notifyListeners) {
        final long coordinateKey = BibPos.toLong(xx, zz);
        final RawObjectLinkedOpenIdentityHashSet<T> set = map.get(coordinateKey);

        if (set == null) {
            throw new IllegalStateException("Expect non-null set in [%d,%d]".formatted(xx, zz));
        }

        if (!set.remove(object)) {
            throw new IllegalStateException("Expect %s in %s ([%d,%d])".formatted(object, set, xx, zz));
        }

        if (set.isEmpty()) {
            map.remove(coordinateKey);
            pooledHashSets.release(set);
        }
        if (notifyListeners && this.removeListener != null) this.removeListener.accept(object, xx, zz);
    }

    private void notifyListenersSorted(T object, int x, int z, Listener<T> addListener, LongArrayList set) {
        set.sort(LongComparators.asLongComparator(Comparator.comparingLong(l -> chebyshevDistance(x, z, BibPos.getChunkX(l), BibPos.getChunkZ(l)))));
        final LongIterator iterator = set.iterator();
        while (iterator.hasNext()) {
            final long pos = iterator.nextLong();
            addListener.accept(object, BibPos.getChunkX(pos), BibPos.getChunkZ(pos));
        }
    }

    private static boolean isInRange(int xLower, int xHigher, int zLower, int zHigher, int x, int z) {
        return x >= xLower && x <= xHigher && z >= zLower && z <= zHigher;
    }

    private static int chebyshevDistance(int x0, int z0, int x1, int z1) {
        return Math.max(Math.abs(x0 - x1), Math.abs(z0 - z1));
    }

    // only for debugging
    private void validate(T object, int x, int z, int viewDistance) {
        if (!DEBUG) return;
        if (viewDistance < 0) {
            for (Long2ObjectMap.Entry<RawObjectLinkedOpenIdentityHashSet<T>> entry : map.long2ObjectEntrySet()) {
                if (entry.getValue().contains(object))
                    throw new IllegalStateException("Unexpected %s in %s ([%d,%d])".formatted(object, entry.getValue(), BibPos.getChunkX(entry.getLongKey()), BibPos.getChunkZ(entry.getLongKey())));
            }
        } else {
            for (int xx = x - viewDistance; xx <= x + viewDistance; xx++) {
                for (int zz = z - viewDistance; zz <= z + viewDistance; zz++) {
                    final long coordinateKey = BibPos.toLong(xx, zz);
                    final RawObjectLinkedOpenIdentityHashSet<T> set = map.get(coordinateKey);
                    if (set == null)
                        throw new IllegalStateException("Expect non-null set in [%d,%d]".formatted(xx, zz));
                    if (!set.contains(object))
                        throw new IllegalStateException("Expect %s in %s ([%d,%d])".formatted(object, set, xx, zz));
                }
            }
        }
    }

    private static class RawObjectLinkedOpenIdentityHashSet<E> extends ReferenceLinkedOpenHashSet<E> {

        public RawObjectLinkedOpenIdentityHashSet() {
        }

        public Object[] getRawSet() {
            return this.key;
        }

        @Override
        protected void rehash(int newN) {
            if (newN > n) { // don't shrink automatically
                super.rehash(newN);
            }
        }
    }

    public interface Listener<T> {
        void accept(T object, int x, int z);
    }

}