package rocks.blackblock.perf.mixin.cache;

import net.minecraft.state.property.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rocks.blackblock.perf.dedup.IdentifierCaches;

/**
 * Deduplicate property names
 * and improve equality checks
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(Property.class)
public class PropertyMixin<T> {

    @Shadow
    @Final
    private String name;

    @Shadow
    @Final
    private Class<T> type;

    @ModifyVariable(
            method = "<init>",
            at = @At(
                    value = "HEAD"
            ),
            ordinal = 0,
            argsOnly = true
    )
    private static String bb$deduplicateName(String name) {
        return IdentifierCaches.PROPERTY.deduplicate(name);
    }

    /**
     * @author embeddedt
     * @reason compare hashcodes if generated, use reference equality for speed
     */
    @Overwrite(remap = false)
    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (!(p_equals_1_ instanceof Property)) {
            return false;
        } else {
            Property<?> property = (Property)p_equals_1_;
            /* reference equality is safe here because of deduplication */
            return this.type == property.getType() && this.name == property.getName();
        }
    }
}
