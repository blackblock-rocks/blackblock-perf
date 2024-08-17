package rocks.blackblock.perf.mixin.reduce_alloc;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.perf.dedup.Constants;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixinForReducedAlloc {

    @Redirect(
        method = "sendPackets",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EquipmentSlot;values()[Lnet/minecraft/entity/EquipmentSlot;"
        )
    )
    private EquipmentSlot[] redirectValues() {
        return Constants.equipmentSlotArray;
    }
}
