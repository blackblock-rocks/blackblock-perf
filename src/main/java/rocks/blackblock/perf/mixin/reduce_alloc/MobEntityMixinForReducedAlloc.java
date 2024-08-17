package rocks.blackblock.perf.mixin.reduce_alloc;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.perf.dedup.Constants;

/**
 * Don't create a new array each time
 * the EquipmentSlot enum is iterated over
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(MobEntity.class)
public class MobEntityMixinForReducedAlloc {

    @Redirect(
        method = "dropEquipment(Ljava/util/function/Predicate;)Ljava/util/Set;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EquipmentSlot;values()[Lnet/minecraft/entity/EquipmentSlot;"
        )
    )
    private EquipmentSlot[] bb$redirectEquipmentSlotArray1() {
        return Constants.equipmentSlotArray;
    }

    @Redirect(
        method = "initEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EquipmentSlot;values()[Lnet/minecraft/entity/EquipmentSlot;"
        )
    )
    private EquipmentSlot[] bb$redirectEquipmentSlotArray2() {
        return Constants.equipmentSlotArray;
    }

    @Redirect(
        method = "updateEnchantments",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EquipmentSlot;values()[Lnet/minecraft/entity/EquipmentSlot;"
        )
    )
    private EquipmentSlot[] bb$redirectEquipmentSlotArray3() {
        return Constants.equipmentSlotArray;
    }

    @Redirect(
        method = "convertTo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EquipmentSlot;values()[Lnet/minecraft/entity/EquipmentSlot;"
        )
    )
    private EquipmentSlot[] bb$redirectEquipmentSlotArray4() {
        return Constants.equipmentSlotArray;
    }
}
