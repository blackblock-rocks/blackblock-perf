package rocks.blackblock.perf.spawn;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnDensityCapper;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import rocks.blackblock.bib.bv.parameter.IntegerParameter;
import rocks.blackblock.bib.bv.parameter.MapParameter;
import rocks.blackblock.bib.bv.value.BvInteger;
import rocks.blackblock.bib.player.BlackblockPlayer;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.perf.BlackblockPerf;
import rocks.blackblock.perf.dynamic.DynamicSetting;
import rocks.blackblock.perf.thread.HasPerformanceInfo;

import java.util.List;

/**
 * The main Dynamic Spawns class
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class DynamicSpawns {

    // The magic spawning number
    private static final int MAGIC_NUMBER = (int) Math.pow(17.0, 2.0);

    // The "spawning" tweak map
    public static final MapParameter<?> SPAWNING_TWEAKS = BlackblockPerf.createTweakMap("spawning");

    // The TweakParameter to use for setting the minimum modifier percentage
    private static final IntegerParameter MIN_MODIFIER_PERCENTAGE_PARAMETER = SPAWNING_TWEAKS.add(new IntegerParameter("min_modifier_percentage"));

    // The minimum modifier percentage
    public static int MIN_MODIFIER_PERCENTAGE = 30;

    // The dynamic setting
    public static DynamicSetting MOBCAP_MODIFIER = new DynamicSetting(
            "Mobcap Modifier",
            BibPerf.State.BUSY,
            0,
            100,
            value -> value + "%",
            DynamicSpawns::updateMobcaps
    );

    /**
     * Initialize the settings
     * @since    0.1.0
     */
    @ApiStatus.Internal
    public static void init() {

        MOBCAP_MODIFIER.setPreferredValue(100);
        MOBCAP_MODIFIER.setPerformanceValue(30);

        // The minimum modifier percentage is 30%
        MIN_MODIFIER_PERCENTAGE_PARAMETER.setDefaultValue(BvInteger.of(30));

        // Listen to changes to the thread count
        MIN_MODIFIER_PERCENTAGE_PARAMETER.addChangeListener(bvIntegerChangeContext -> {
            int min_modifier = bvIntegerChangeContext.getValue().getFlooredInteger();
            DynamicSpawns.setNewMinModifierPercentage(min_modifier);
        });
    }

    /**
     * Set the new minimum modifier percentage
     * @since    0.1.0
     */
    public static void setNewMinModifierPercentage(int new_min_modifier_percentage) {

        if (MIN_MODIFIER_PERCENTAGE == new_min_modifier_percentage) {
            return;
        }

        DynamicSpawns.MIN_MODIFIER_PERCENTAGE = new_min_modifier_percentage;

        BibLog.attention("Minimum modifier percentage:", DynamicSpawns.MIN_MODIFIER_PERCENTAGE);
    }

    /**
     * Update the mobcaps in the given world
     * @since    0.1.0
     */
    public static void updateMobcaps(World world, int modifier) {

        double modifier_percentage = modifier / 100.0;

        for (SpawnGroup group : SpawnGroup.values()) {
            group.bb$setCapacityModifier(world, modifier_percentage);
        }
    }

    /**
     * Can the given entity spawn in the given world?
     * @since    0.1.0
     */
    public static boolean canSpawn(Entity entity, World world, BlockPos pos) {
        return !(world instanceof ServerWorld server_world) || canSpawn(entity, server_world, pos);
    }

    /**
     * Can the given entity spawn in the given world?
     * @since    0.1.0
     */
    public static boolean canSpawn(Entity entity, ServerWorld world, BlockPos pos) {
        var spawn_group = entity.getType().getSpawnGroup();
        return canSpawn(spawn_group, world, pos);
    }

    /**
     * Can the given spawn group spawn in the given world?
     * @since    0.1.0
     */
    public static boolean canSpawn(SpawnGroup spawn_group, ServerWorld world, BlockPos pos) {
        SpawnHelper.Info state = world.getChunkManager().getSpawnInfo();

        if (state == null || spawn_group == SpawnGroup.MISC) {
            return true;
        }

        int spawnable_chunk_count = state.getSpawningChunkCount();
        final int capacity = spawn_group.bb$getCapacity(world);
        int world_capacity = capacity * spawnable_chunk_count / MAGIC_NUMBER;

        final int world_count = state.getGroupToCount().getInt(spawn_group);

        if (world_count >= world_capacity) {
            return false;
        }

        var info = ((HasPerformanceInfo) world).bb$getPerformanceInfo();

        for (ServerPlayerEntity player : getPlayersNear(world, new ChunkPos(pos))) {

            if (((BlackblockPlayer) player).bb$isAfk() && info.isRandomlyDisabled()) {
                continue;
            }

            SpawnDensityCapper.DensityCap mob_count = state.densityCapper.playersToDensityCap.get(player);

            if (mob_count == null) {
                return true;
            }

            int density = mob_count.spawnGroupsToDensity.getOrDefault(spawn_group, 0);

            if (density < capacity) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get all the players near the given position
     * @since    0.1.0
     */
    public static List<ServerPlayerEntity> getPlayersNear(ServerWorld world, ChunkPos pos) {
        return world.getChunkManager().chunkLoadingManager.getPlayersWatchingChunk(pos);
    }

    /**
     * Does the given chunk have non-afk players near?
     * @since    0.1.0
     */
    public static boolean hasActivePlayersNear(ServerWorld world, ChunkPos pos) {

        for (ServerPlayerEntity player : getPlayersNear(world, pos)) {
            if (!((BlackblockPlayer) player).bb$isAfk()) {
                return true;
            }
        }

        return false;
    }
}
