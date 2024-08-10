package rocks.blackblock.perf.activation_range;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import rocks.blackblock.bib.player.BlackblockPlayer;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.perf.thread.HasPerformanceInfo;

import java.util.function.Predicate;

/**
 * Activation Range class
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class DynamicActivationRange {

    public static final boolean DO_VILLAGER_PANIC = true;
    public static int VILLAGER_WORK_IMMUNITY_AFTER = 20;
    public static int VILLAGER_WORK_IMMUNITY_FOR = 20;
    private static final double MINIMUM_MOVEMENT = 0.001;
    private static final Predicate<Goal> BEE_GOAL_IMMUNITIES = goal -> goal instanceof BeeEntity.MoveToFlowerGoal || goal instanceof BeeEntity.MoveToHiveGoal;
    private static final Activity[] VILLAGER_PANIC_IMMUNITIES = {
            Activity.HIDE,
            Activity.PRE_RAID,
            Activity.RAID,
            Activity.PANIC
    };

    // The default activation range
    private static final ActivationRange DEFAULT_RANGE = ActivationRange.build("default")
            .setActivationRange(10, 16)
            .setActiveTickDelay(18, 18)
            .build();

    // The activation range for illagers
    private static final ActivationRange ILLAGER_RANGE = ActivationRange.build("illager")
            .setActivationRange(24, 48)
            .setActiveTickDelay(18, 20)
            .setInactiveWakeupInterval(20)
            .setVerticalRangeExtraHeight(64)
            .setVerticalRangeExtraHeightDown(0)
            .build();

    // The activation range for water mobs
    private static final ActivationRange WATER_RANGE = ActivationRange.build("water")
            .setActivationRange(8, 16)
            .setActiveTickDelay(10, 18)
            .setInactiveWakeupInterval(60)
            .setVerticalRangeExtraHeight(0)
            .setVerticalRangeExtraHeightDown(0)
            .build();

    // The activation range for villagers
    private static final ActivationRange VILLAGER_RANGE = ActivationRange.build("villager")
            .setActivationRange(8, 16)
            .setActiveTickDelay(10, 18)
            .setInactiveWakeupInterval(30)
            .setVerticalRangeExtraHeight(0)
            .setVerticalRangeExtraHeightDown(0)
            .build();

    // The activation range for zombies
    private static final ActivationRange ZOMBIE_RANGE = ActivationRange.build("zombie")
            .setActivationRange(12, 16)
            .setActiveTickDelay(15, 20)
            .setInactiveWakeupInterval(20)
            .setVerticalRangeExtraHeight(64)
            .setVerticalRangeExtraHeightDown(0)
            .build();

    // The activation range for "subterranean" mobs (creepers, slime, magma cubes, hoglin)
    private static final ActivationRange SUB_MONSTER_RANGE = ActivationRange.build("monsters-sub")
            .setActivationRange(16, 32)
            .setActiveTickDelay(15, 20)
            .setInactiveWakeupInterval(20)
            .setVerticalRangeExtraHeight(64)
            .setVerticalRangeExtraHeightDown(64)
            .build();

    // The activation range for flying mobs (ghasts and phantoms)
    private static final ActivationRange FLYING_RANGE = ActivationRange.build("flying-monsters")
            .setActivationRange(32, 48)
            .setActiveTickDelay(15, 20)
            .setInactiveWakeupInterval(20)
            .setVerticalRangeExtraHeight(64)
            .setVerticalRangeExtraHeightDown(0)
            .build();

    // The activation range for the rest of the mobs
    private static final ActivationRange REST_MONSTER_RANGE = ActivationRange.build("monsters-other")
            .setActivationRange(16, 32)
            .setActiveTickDelay(15, 20)
            .setInactiveWakeupInterval(20)
            .setVerticalRangeExtraHeight(64)
            .setVerticalRangeExtraHeightDown(0)
            .build();

    // The activation range for animals
    private static final ActivationRange ANIMAL_RANGE = ActivationRange.build("animals")
            .setActivationRange(8, 16)
            .setActiveTickDelay(10, 18)
            .setInactiveWakeupInterval(60)
            .setVerticalRangeExtraHeight(0)
            .setVerticalRangeExtraHeightDown(0)
            .build();

    // The activation range for creatures
    private static final ActivationRange CREATURE_RANGE = ActivationRange.build("creatures")
            .setActivationRange(16, 24)
            .setActiveTickDelay(10, 20)
            .setInactiveWakeupInterval(30)
            .setVerticalRangeExtraHeight(0)
            .setVerticalRangeExtraHeightDown(0)
            .build();

    /**
     * Is activation range enabled for the given entity?
     * @since 0.1.0
     */
    public static boolean appliesTo(Entity entity) {

        var type = entity.getType();

        // Don't use custom activation range for dragons, withers and wardens
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.WARDEN) {
            return false;
        }

        if (type == EntityType.PLAYER) {
            return false;
        }

        if (entity instanceof ProjectileEntity
                || entity instanceof ThrownEntity
                || entity instanceof EnderDragonPart
                || entity instanceof FireballEntity
                || entity instanceof LightningEntity
                || entity instanceof TntEntity
                || entity instanceof EndCrystalEntity
                || entity instanceof FireworkRocketEntity
                || entity instanceof EyeOfEnderEntity
                || entity instanceof TridentEntity
        ) {
            return false;
        }

        return true;
    }

    /**
     * Get the activation range for the given entity
     * @since 0.1.0
     */
    @Nullable
    public static ActivationRange getActivationRange(Entity entity) {

        if (!appliesTo(entity)) {
            return null;
        }

        if (entity instanceof MerchantEntity) {
            return VILLAGER_RANGE;
        }

        if (entity instanceof ZombieEntity) {
            return ZOMBIE_RANGE;
        }

        if (entity instanceof FlyingEntity) {
            return FLYING_RANGE;
        }

        if (entity instanceof IllagerEntity) {
            return ILLAGER_RANGE;
        }

        var type = entity.getType();

        if (type == EntityType.CREEPER || type == EntityType.SLIME || type == EntityType.MAGMA_CUBE || type == EntityType.HOGLIN) {
            return SUB_MONSTER_RANGE;
        }

        if (entity instanceof Monster) {
            return REST_MONSTER_RANGE;
        }

        if (entity instanceof WaterCreatureEntity) {
            return WATER_RANGE;
        }

        if (entity instanceof AnimalEntity || entity instanceof AmbientEntity) {
            return ANIMAL_RANGE;
        }

        if (entity instanceof MobEntity) {
            return CREATURE_RANGE;
        }

        return DEFAULT_RANGE;
    }

    /**
     * Trigger activation for entities in range of a player
     * @since 0.1.0
     */
    public static void triggerActivation(ServerWorld world, int current_tick) {

        int max_range = Integer.MIN_VALUE;

        // Check all the activation ranges to determine the maximum range
        for (ActivationRange activation_range : ActivationRange.ACTIVATION_RANGES) {
            max_range = Math.max(activation_range.getActivationRange(world), max_range);
        }

        // Also use the view distance
        // @TODO: Make view distance depend on the world
        max_range = Math.max(world.getServer().getPlayerManager().getViewDistance(), max_range);

        // Get the world perf info
        BibPerf.Info info = ((HasPerformanceInfo) world).bb$getPerformanceInfo();

        for (ServerPlayerEntity player : world.getPlayers()) {

            // Ignore spectators
            if (player.isSpectator()) {
                continue;
            }

            // Ignore AFK players when the server is busy
            if ((info.isRandomlyDisabled() || info.isCritical()) && ((BlackblockPlayer) player).bb$isAfk()) {
                continue;
            }

            Box max_box = player.getBoundingBox().expand(max_range, 256, max_range);

            for (Entity entity : world.getOtherEntities(player, max_box)) {
                activateEntityIfNeeded(player, entity, current_tick, DEFAULT_RANGE);
            }
        }
    }

    public static void activateEntityIfNeeded(ServerPlayerEntity player, Entity entity, int current_tick, ActivationRange global_config) {

        if (current_tick < entity.bb$getActivatedUntilTick()) {
            return;
        }

        if (entity.bb$isExcludedFromDynamicActivationRange() || isWithinRange(player, entity, global_config)) {
            entity.bb$setActivatedUntilTick(current_tick + 19);
        }
    }

    private static boolean isWithinRange(ServerPlayerEntity player, Entity entity, ActivationRange global_config) {
        final ActivationRange type = entity.bb$getActivationRange();
        final int range = type.getActivationRange(player.getWorld());

        final int manhattan_distance = Math.max(
                Math.abs(player.getBlockX() - entity.getBlockX()),
                Math.abs(player.getBlockZ() - entity.getBlockZ())
        );

        if (manhattan_distance > range) {
            return false;
        }

        boolean use_vertical_range = type.isAllowVerticalRange();

        if (use_vertical_range) {
            final int deltaY = entity.getBlockY() - player.getBlockY();
            return deltaY <= range && deltaY >= -range
                    || (deltaY > 0 && type.useExtraHeightUp())
                    || (deltaY < 0 && type.useExtraHeightDown());
        }

        return true;
    }

    /**
     * Should the given entity be ticked?
     * @since 0.1.0
     */
    private static boolean shouldTick(Entity entity, ActivationRange config) {
        return entity.bb$isExcludedFromDynamicActivationRange() || entity.hasPortalCooldown()
                || (entity.portalManager != null && entity.portalManager.isInPortal()) // Entities in portals
                || (entity.age < 200 && config.tickNewEntities()) // New entities
                || (entity instanceof Leashable leashable && leashable.getLeashData() != null && leashable.getLeashData().leashHolder instanceof PlayerEntity) // Player leashed mobs
                || (entity instanceof LivingEntity living && living.hurtTime > 0); // Attacked mobs
    }

    /**
     * Is the given entity active?
     * @since 0.1.0
     */
    public static boolean checkIfActive(Entity entity, int current_tick) {

        if (shouldTick(entity, DEFAULT_RANGE)) {
            entity.bb$setActivatedUntilTick(current_tick);
            return true;
        }

        boolean active = entity.bb$getActivatedUntilTick() >= current_tick;

        if (!active) {
            final int inactiveTicks = current_tick - entity.bb$getActivatedUntilTick() - 1;
            if (inactiveTicks % 20 == 0) {
                // Check immunities every 20 inactive ticks.
                final int immunity = checkEntityImmunities(entity, current_tick, DEFAULT_RANGE);
                if (immunity >= 0) {
                    entity.bb$setActivatedUntilTick(current_tick + immunity);
                    return true;
                }
            }

            final int tickInterval = entity.bb$getActivationRange().getActiveTickDelay(entity.getWorld());

            if (tickInterval > 0 && inactiveTicks % tickInterval == 0) {
                return true;
            }
        }

        return active;
    }

    /**
     * Return the amount of ticks an entity should be immune for activation range checks.
     * @since    0.1.0
     */
    public static int checkEntityImmunities(Entity entity, int currentTick, ActivationRange config) {

        final int inactiveWakeUpImmunity = checkInactiveWakeup(entity, currentTick);
        if (inactiveWakeUpImmunity > -1) {
            return inactiveWakeUpImmunity;
        }

        if (entity.getFireTicks() > 0) {
            return 2;
        }

        if (entity.bb$getImmuneUntilTick() >= currentTick) {
            return 1;
        }

        if (!entity.isAlive()) {
            return 40;
        }

        // quick checks.
        if (entity.isInFluid() && entity.isPushedByFluids() && !(entity instanceof PassiveEntity || entity instanceof VillagerEntity || entity instanceof BoatEntity)) {
            return 100;
        }

        // ServerCore - Immunize moving items & xp orbs.
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity) {
            final Vec3d movement = entity.getVelocity();
            if (Math.abs(movement.x) > MINIMUM_MOVEMENT || Math.abs(movement.z) > MINIMUM_MOVEMENT || movement.y > MINIMUM_MOVEMENT) {
                return 20;
            }
        }

        if (!(entity instanceof PersistentProjectileEntity projectile)) {
            if (!entity.isOnGround() && !entity.isInFluid() && !(entity instanceof FlyingEntity || entity instanceof BatEntity)) {
                return 10;
            }
        } else if (!projectile.inGround) {
            return 1;
        }

        // special cases.
        if (entity instanceof LivingEntity living) {
            if (living.jumping || !living.getStatusEffects().isEmpty() || living.isClimbing()) {
                return 1;
            }

            if (living instanceof MobEntity mob) {
                if (mob.getTarget() != null || mob.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET)) {
                    return 20;
                }


                if (mob instanceof BeeEntity bee && (bee.pollinateGoal.isRunning() || bee.hasAngerTime() || hasTasks(bee.getGoalSelector(), BEE_GOAL_IMMUNITIES))) {
                    return 20;
                }

                if (mob instanceof VillagerEntity villager) {
                    Brain<VillagerEntity> brain = villager.getBrain();

                    if (DO_VILLAGER_PANIC) {
                        for (Activity activity : VILLAGER_PANIC_IMMUNITIES) {
                            if (brain.hasActivity(activity)) {
                                return 20 * 5;
                            }
                        }
                    }

                    final int immunityAfter = VILLAGER_WORK_IMMUNITY_AFTER;
                    if (immunityAfter > 0 && (currentTick - mob.bb$getActivatedUntilTick()) >= immunityAfter) {
                        if (brain.hasActivity(Activity.WORK)) {
                            return VILLAGER_WORK_IMMUNITY_FOR;
                        }
                    }
                }

                if (mob instanceof LlamaEntity llama && llama.isFollowing()) {
                    return 1;
                }

                if (mob instanceof AnimalEntity animal) {
                    if (animal.isBaby() || animal.isInLove()) {
                        return 5;
                    }

                    if (mob instanceof SheepEntity sheep && sheep.isSheared()) {
                        return 1;
                    }
                }

                if (mob instanceof CreeperEntity creeper && creeper.isIgnited()) {
                    return 20;
                }

                if (hasTasks(mob.targetSelector, null)) {
                    return 0;
                }
            }
        }

        return -1;
    }

    /**
     * Get the amount of ticks a currently inactive entity should be woken up for
     * @since 0.1.0
     */
    private static int checkInactiveWakeup(Entity entity, int current_tick) {

        ActivationRange range = entity.bb$getActivationRange();

        if (!range.wakeupAfterInactiveTicks()) {
            return -1;
        }

        int wakeup_interval = range.getInactiveWakeupInterval();

        if (wakeup_interval > 0 && current_tick - entity.bb$getActivatedUntilTick() >= wakeup_interval * 20L) {
            return 100;
        }

        return -1;
    }

    public static boolean hasTasks(GoalSelector selector, @Nullable Predicate<Goal> predicate) {
        for (var wrapped : selector.getGoals()) {
            if (wrapped.isRunning() && (predicate == null || predicate.test(wrapped.getGoal()))) {
                return true;
            }
        }
        return false;
    }

}
