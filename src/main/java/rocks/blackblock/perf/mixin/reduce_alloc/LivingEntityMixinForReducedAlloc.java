package rocks.blackblock.perf.mixin.reduce_alloc;

import com.moulberry.mixinconstraints.annotations.IfModAbsent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.perf.dedup.Constants;

@IfModAbsent("lithium")
@Mixin(LivingEntity.class)
public class LivingEntityMixinForReducedAlloc {

    @Redirect(
        method = "getEquipmentChanges",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EquipmentSlot;values()[Lnet/minecraft/entity/EquipmentSlot;"
        )
    )
    private EquipmentSlot[] redirectValues() {
        return Constants.equipmentSlotArray;
    }
}
