package rocks.blackblock.perf.dedup;

import rocks.blackblock.perf.util.DeduplicationCache;

public class IdentifierCaches {
    public static final DeduplicationCache<String> NAMESPACES = new DeduplicationCache<>();
    public static final DeduplicationCache<String> PATH = new DeduplicationCache<>();
    public static final DeduplicationCache<String> PROPERTY = new DeduplicationCache<>();
}
