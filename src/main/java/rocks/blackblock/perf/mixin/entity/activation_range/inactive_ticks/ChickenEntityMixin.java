package rocks.blackblock.perf.mixin.entity.activation_range.inactive_ticks;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Make chickens lay eggs even when they're inactive
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ChickenEntity.class)
public abstract class ChickenEntityMixin extends AnimalEntity {

    @Shadow
    public boolean hasJockey;

    @Shadow
    public int eggLayTime;

    protected ChickenEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void bb$inactiveTick() {
        super.bb$inactiveTick();

        if (!this.hasJockey && this.age >= 0 && this.isAlive() && --this.eggLayTime <= 0 && this.getWorld() instanceof ServerWorld serverWorld) {

            if (this.forEachGiftedItem(serverWorld, LootTables.CHICKEN_LAY_GAMEPLAY, this::dropStack)) {
                this.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                this.emitGameEvent(GameEvent.ENTITY_PLACE);
            }

            this.eggLayTime = this.random.nextInt(6000) + 6000;
        }
    }
}
