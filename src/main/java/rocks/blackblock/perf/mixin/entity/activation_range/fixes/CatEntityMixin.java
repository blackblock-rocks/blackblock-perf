package rocks.blackblock.perf.mixin.entity.activation_range.fixes;

import net.minecraft.entity.passive.CatEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fix cats not despawning properly
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(CatEntity.class)
public class CatEntityMixin {

    @Redirect(
            method = "canImmediatelyDespawn",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/passive/CatEntity;age:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int bb$fixCatDespawning(CatEntity cat) {
        return cat.bb$getPotentialTickCount();
    }
}
