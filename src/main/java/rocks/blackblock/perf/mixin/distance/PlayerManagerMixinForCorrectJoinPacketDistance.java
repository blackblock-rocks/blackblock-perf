package rocks.blackblock.perf.mixin.distance;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Make sure the player gets the correct view distance
 * when they join the server
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(PlayerManager.class)
public class PlayerManagerMixinForCorrectJoinPacketDistance {

    @Redirect(
            method = "onPlayerConnect",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/server/PlayerManager;viewDistance:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int modifyViewDistance(PlayerManager instance, @Local ServerPlayerEntity player) {
        return player.bb$getWorldViewDistance();
    }
}
