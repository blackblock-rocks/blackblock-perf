package rocks.blackblock.perf.mixin.entity.breeding;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.perf.activation_range.EntityCluster;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Prevent animals from breeding too many mobs
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(AnimalMateGoal.class)
public class AnimalMateGoalMixin {

    private static final Predicate<Entity> NON_PLAYER_LIVING_ENTITIES = entity -> {
        if (!entity.isAlive()) {
            return false;
        }

        if (entity instanceof LivingEntity livingEntity) {

            if (entity instanceof PlayerEntity) {
                return false;
            }

            return true;
        }

        return false;
    };

    @Shadow @Final protected AnimalEntity animal;

    @Shadow @Nullable protected AnimalEntity mate;

    @Inject(method = "breed", at = @At("HEAD"), cancellable = true)
    private void bb$onBreed(CallbackInfo ci) {

        var cluster = this.animal.bb$getCluster();
        boolean allowed = true;

        if (cluster != null) {
            if (cluster.getSize() > 32) {
                allowed = false;
            } else {
                EntityCluster super_cluster = cluster.getSuperCluster();
                if (super_cluster != null) {
                    if (super_cluster.getSize() > 42) {
                        allowed = false;
                    }
                }
            }
        }

        if (allowed) {
            Box box = this.animal.getBoundingBox().expand(18);
            int entities_in_range = this.animal.getWorld().getOtherEntities(this.animal, box, NON_PLAYER_LIVING_ENTITIES).size();
            allowed = entities_in_range < 100;
        }

        if (!allowed) {
            this.animal.resetLoveTicks();
            this.animal.setBreedingAge(6000);
            this.mate.resetLoveTicks();
            this.mate.setBreedingAge(6000);
            ci.cancel();

            Optional.ofNullable(this.animal.getLovingPlayer()).or(() -> Optional.ofNullable(this.mate.getLovingPlayer())).ifPresent(player -> {
                player.sendMessage(Text.literal("Breeding aborted: too many entities in range"), true);
            });
        }
    }

}
