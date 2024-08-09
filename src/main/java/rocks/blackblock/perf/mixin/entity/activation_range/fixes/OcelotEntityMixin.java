package rocks.blackblock.perf.mixin.entity.activation_range.fixes;

import net.minecraft.entity.passive.OcelotEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fix ocelots not despawning properly
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(OcelotEntity.class)
public class OcelotEntityMixin {

    @Redirect(
            method = "canImmediatelyDespawn",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/passive/OcelotEntity;age:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int bb$fixOcelotDespawning(OcelotEntity ocelot) {
        return ocelot.bb$getPotentialTickCount();
    }
}
