package rocks.blackblock.perf.mixin.spawn;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocks.blackblock.perf.spawn.DynamicSpawns;

/**
 * Prevent zombie reinforcements from spawning too many mobs
 * when the world is overloaded
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ZombieEntity.class)
public abstract class ZombieMixin extends MobEntity {

    protected ZombieMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyExpressionValue(
            method = "damage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"
            )
    )
    private boolean bb$enforceMobCap(boolean doMobSpawning, @Local(ordinal = 0) ServerWorld world) {
        return doMobSpawning && DynamicSpawns.canSpawn(
                EntityType.ZOMBIE.getSpawnGroup(),
                world,
                this.getChunkPos().getStartPos()
        );
    }
}
