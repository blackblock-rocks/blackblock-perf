package rocks.blackblock.perf.util;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentLong2ByteOpenHashMap extends Long2ByteOpenHashMap {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public byte put(long key, byte value) {
        lock.writeLock().lock();
        try {
            return super.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public byte get(long key) {
        lock.readLock().lock();
        try {
            return super.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public byte remove(long key) {
        lock.writeLock().lock();
        try {
            return super.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
