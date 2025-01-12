package rocks.blackblock.perf.mixin.entity.despawn_holding_garbage;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.bib.util.BibEntity;
import rocks.blackblock.bib.util.BibItem;
import rocks.blackblock.bib.util.BibLog;

/**
 * Don't make mobs persistent just because they picked up garbage
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {

    @Shadow private boolean persistent;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * See if the mob is picking up garbage.
     * If it is, cancel setting the persistence.
     * @since    0.1.0
     */
    @Inject(
        method = "equipLootStack",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/mob/MobEntity;persistent:Z",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void bb$modifyPersistentValue(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {

        boolean is_garbage = BibItem.isGarbage(stack);

        // Allow the mob to be turned persistent
        // when it's picking up something that's not garbage
        if (!is_garbage) {
            return;
        }

        boolean is_persistent = this.persistent;

        // If it's already persistent, see if it's holding something that's not garbage
        if (is_persistent) {

            // Don't prevent persistency when the mob has a custom name
            if (this.hasCustomName()) {
                return;
            }

            // If it's not only holding garbage, don't prevent persistency
            if (!BibEntity.isOnlyHoldingGarbage(this)) {
                return;
            }
        }

        ci.cancel();
    }

    /**
     * If a mob is being loaded, and it's persistent,
     * disable the persistency if it's just because it's holding garbage.
     *
     * @since    0.1.0
     */
    @Inject(
        method = "readCustomDataFromNbt",
        at = @At(
            value = "RETURN"
        )
    )
    private void bb$afterReadNbt(CallbackInfo ci) {

        // Nothing needs to be done if the mob is not persistent
        if (!this.persistent) {
            return;
        }

        // Keep it persistent if it has a custom name
        if (this.hasCustomName()) {
            return;
        }

        // We no longer do this, because some monsters
        // can be holding garbage (or nothing) and be persistent.
        // Like a breeze
    }
}
