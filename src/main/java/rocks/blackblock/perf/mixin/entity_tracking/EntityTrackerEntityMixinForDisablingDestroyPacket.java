package rocks.blackblock.perf.mixin.entity_tracking;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Don't send an `EntitiesDestroyS2CPacket` when the player is disconnecting
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntityMixinForDisablingDestroyPacket {

    @WrapWithCondition(
        method = "stopTracking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"
        )
    )
    private boolean onlySendIfPlayerIsStayingConnected(ServerPlayNetworkHandler instance, Packet packet, ServerPlayerEntity player) {
        return !instance.bb$isDisconnecting();
    }
}
