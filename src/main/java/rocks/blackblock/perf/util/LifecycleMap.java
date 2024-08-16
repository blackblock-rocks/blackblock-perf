package rocks.blackblock.perf.util;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntryInfo;

/**
 * Optimized map for storing Lifecycle values,
 * using reference equality and not storing the default value.
 *
 * @author   embeddedt
 * @since    0.1.0
 */
public class LifecycleMap<T> extends Reference2ReferenceOpenHashMap<RegistryKey<T>, RegistryEntryInfo> {

    public LifecycleMap() {
        this.defaultReturnValue(RegistryEntryInfo.DEFAULT);
    }

    @Override
    public RegistryEntryInfo put(RegistryKey<T> t, RegistryEntryInfo lifecycle) {

        if (lifecycle != defRetValue) {
            return super.put(t, lifecycle);
        }

        // need the duplicate containsKey/get logic here to override the default return value
        return super.containsKey(t) ? super.get(t) : null;
    }
}
