package rocks.blackblock.perf.mixin.compact_mojang_registries;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.util.LifecycleMap;

import java.util.Map;

/**
 * Use an optimized map for storing entry info,
 * which improves performance and reduces memory usage.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(SimpleRegistry.class)
public class SimpleRegistryMixin<T> {

    @Shadow
    private Map<RegistryKey<T>, RegistryEntryInfo> keyToEntryInfo;

    @Inject(
        method = "<init>(Lnet/minecraft/registry/RegistryKey;Lcom/mojang/serialization/Lifecycle;Z)V",
        at = @At("RETURN")
    )
    private void replaceStorage(CallbackInfo ci) {
        this.keyToEntryInfo = new LifecycleMap<>();
    }
}
