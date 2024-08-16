package rocks.blackblock.perf.mixin.cache;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rocks.blackblock.perf.dedup.IdentifierCaches;

/**
 * Try to deduplicate the namespace and path of Identifiers
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(Identifier.class)
public class IdentifierMixin {

    @ModifyVariable(
        method = "<init>",
        at = @At(
            value = "HEAD"
        ),
        ordinal = 0,
        argsOnly = true
    )
    private static String bb$internNamespace(String namespace) {
        // Namespaces are usually very short,
        // so interning them is a good idea
        return namespace.intern();
    }

    @ModifyVariable(
        method = "<init>",
        at = @At(
            value = "HEAD"
        ),
        ordinal = 1,
        argsOnly = true
    )
    private static String bb$internPath(String path) {
        // Paths can be longer, so use our own cache
        return IdentifierCaches.PATH.deduplicate(path);
    }
}
