package rocks.blackblock.perf.distance;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.server.network.ChunkFilter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.blackblock.bib.util.BibPos;

import java.util.Arrays;
import java.util.Set;

/**
 * A replacement class for {@link PlayerChunkWatchingManager}:
 * Uses a more efficient data structure (AreaMap) for tracking player-chunk relationships.
 * Optimizes the process of finding players watching a specific chunk.
 * Reduces the overhead of updating player positions and view distances.
 *
 * Based on the work in VMP by ishland
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class AreaPlayerChunkWatchingManager {

    // Listener no-op
    private static final Listener NOOP = (a, b, c) -> {};

    // The maximum distance at which entities can be immediately despawned, based on spawn group settings.
    public static final int GENERAL_PLAYER_AREA_MAP_DISTANCE = (int) Math.ceil(
            Arrays.stream(SpawnGroup.values())
                    .mapToInt(SpawnGroup::getImmediateDespawnRange)
                    .reduce(0, Math::max) / 16.0
    );

    // AreaMap for tracking players within their specific view distances
    private final AreaMap<ServerPlayerEntity> playerAreaMap;

    // AreaMap for tracking players within a general area, used for entity spawning and despawning
    private final AreaMap<ServerPlayerEntity> generalPlayerAreaMap = new AreaMap<>();

    // Map to store the last known position of each player
    private final Object2LongOpenHashMap<ServerPlayerEntity> positions = new Object2LongOpenHashMap<>();

    // The server chunk loading manager
    private final ServerChunkLoadingManager server_chunk_loading_manager;

    // The server world
    private final ServerWorld server_world;

    // Listener for when a player starts watching a chunk
    private Listener addListener = NOOP;

    // Listener for when a player stops watching a chunk
    private Listener removeListener = NOOP;

    // The current server-wide view distance
    // @TODO: Make this respect the per-world dynamic view distance setting
    private int watchDistance = 5;

    /**
     * Constructs a new AreaPlayerChunkWatchingManager.
     * @since 0.1.0
     */
    public AreaPlayerChunkWatchingManager() {
        this(null, null, null);
    }

    /**
     * Constructs a new AreaPlayerChunkWatchingManager with specified listeners and chunk loading manager.
     * @since 0.1.0
     *
     * @param addListener                      Listener for when a player starts watching a chunk
     * @param removeListener                   Listener for when a player stops watching a chunk
     * @param server_chunk_loading_manager     The ServerChunkLoadingManager instance
     */
    public AreaPlayerChunkWatchingManager(Listener addListener, Listener removeListener, ServerChunkLoadingManager server_chunk_loading_manager) {
        this.addListener = addListener == null ? NOOP : addListener;
        this.removeListener = removeListener == null ? NOOP : removeListener;
        this.server_chunk_loading_manager = server_chunk_loading_manager;
        this.server_world = server_chunk_loading_manager.world;

        this.playerAreaMap = new AreaMap<>(
                (object, x, z) -> this.addListener.accept(object, x, z),
                (object, x, z) -> this.removeListener.accept(object, x, z),
                true);
    }

    /**
     * Updates player chunk watching based on client view distance changes.
     * @since 0.1.0
     */
    public void tick() {

        boolean new_fifth_second = this.server_world.getTime() % 100 == 0;

        for (Object2LongMap.Entry<ServerPlayerEntity> entry : this.positions.object2LongEntrySet()) {
            final ServerPlayerEntity player = entry.getKey();

            if (new_fifth_second) {
                // Recalculate the personal view distance every 5 seconds
                player.bb$recalculatePersonalViewDistance();
            }

            if (player.bb$hasDirtyPersonalViewDistance()) {
                player.bb$getPersonalViewDistance();
                int distance = this.getViewDistance(player);

                final long pos = entry.getLongValue();
                player.setChunkFilter(ChunkFilter.cylindrical(new ChunkPos(pos), distance));
                this.movePlayer(pos, player);

                // Tell the client the server-side view distance has changed.
                var update_packet = new ChunkLoadDistanceS2CPacket(distance);
                player.networkHandler.sendPacket(update_packet);
            }
        }
    }

    /**
     * Sets the server-wide watch distance and updates all players accordingly.
     * @since 0.1.0
     */
    public void setWatchDistance(int watchDistance) {
        this.watchDistance = Math.max(2, watchDistance);

        final ObjectIterator<Object2LongMap.Entry<ServerPlayerEntity>> iterator = positions.object2LongEntrySet().fastIterator();
        while (iterator.hasNext()) {
            final Object2LongMap.Entry<ServerPlayerEntity> entry = iterator.next();

            entry.getKey().setChunkFilter(ChunkFilter.cylindrical(new ChunkPos(entry.getLongValue()), this.getViewDistance(entry.getKey())));

            this.playerAreaMap.update(
                    entry.getKey(),
                    BibPos.getChunkX(entry.getLongValue()),
                    BibPos.getChunkZ(entry.getLongValue()),
                    getViewDistance(entry.getKey()));

            this.generalPlayerAreaMap.update(
                    entry.getKey(),
                    BibPos.getChunkX(entry.getLongValue()),
                    BibPos.getChunkZ(entry.getLongValue()),
                    GENERAL_PLAYER_AREA_MAP_DISTANCE);
        }
    }

    /**
     * Gets the current server-wide watch distance.
     * @since 0.1.0
     */
    public int getWatchDistance() {
        return this.watchDistance;
    }

    /**
     * Get all the players being watched by this manager.
     * @since 0.1.0
     */
    public Set<ServerPlayerEntity> getAllPlayers() {
        return this.playerAreaMap.getAllObjects();
    }

    /**
     * Gets the set of players watching a specific chunk.
     * @since 0.1.0
     */
    public Set<ServerPlayerEntity> getPlayersWatchingChunk(long long_pos) {
        return this.playerAreaMap.getObjectsInRange(long_pos);
    }

    /**
     * Gets an array of players watching a specific chunk.
     * @since 0.1.0
     */
    public Object[] getPlayersWatchingChunkArray(long long_pos) {
        return this.playerAreaMap.getObjectsInRangeArray(long_pos);
    }

    /**
     * Gets an array of players in the general area of a chunk.
     * @since 0.1.0
     */
    public Object[] getPlayersInGeneralAreaMap(long long_pos) {
        return this.generalPlayerAreaMap.getObjectsInRangeArray(long_pos);
    }

    /**
     * Adds a player to the chunk watching system.
     *
     * @param player The player to add
     * @param pos    The chunk position of the player
     */
    public void add(ServerPlayerEntity player, long pos) {

        final int x = ChunkPos.getPackedX(pos);
        final int z = ChunkPos.getPackedZ(pos);

        this.playerAreaMap.add(player, x, z, this.getViewDistance(player));
        this.generalPlayerAreaMap.add(player, x, z, GENERAL_PLAYER_AREA_MAP_DISTANCE);

        this.positions.put(player, BibPos.toLong(x, z));
    }

    /**
     * Removes a player from the chunk watching system.
     * @since 0.1.0
     */
    public void remove(ServerPlayerEntity player) {

        this.playerAreaMap.remove(player);
        this.generalPlayerAreaMap.remove(player);

        this.positions.removeLong(player);
    }

    /**
     * Updates a player's position in the chunk watching system.
     *
     * @param new_long_pos The new chunk position of the player
     * @param player       The player to update
     */
    public void movePlayer(long new_long_pos, ServerPlayerEntity player) {

        final int x = ChunkPos.getPackedX(new_long_pos);
        final int z = ChunkPos.getPackedZ(new_long_pos);

        this.playerAreaMap.update(player, x, z, this.getViewDistance(player));
        this.generalPlayerAreaMap.update(player, x, z, GENERAL_PLAYER_AREA_MAP_DISTANCE);

        this.positions.put(player, BibPos.toLong(x, z));
    }

    /**
     * Get the player's last updated position
     * (Used for debugging)
     *
     * @param player The player to calculate for
     */
    @Nullable
    public ChunkPos getPlayerChunkPosition(ServerPlayerEntity player) {

        if (!this.positions.containsKey(player)) {
            return null;
        }

        long pos = this.positions.getLong(player);
        ChunkPos chunkPos = new ChunkPos(pos);

        return chunkPos;
    }

    /**
     * Calculates the effective view distance for a player.
     *
     * @param player The player to calculate for
     */
    public int getViewDistance(ServerPlayerEntity player) {
        return MathHelper.clamp(player.bb$getPersonalViewDistance(), 2, this.watchDistance);
    }

    /**
     * Interface for listeners that respond to chunk watching changes.
     * @since 0.1.0
     */
    @FunctionalInterface
    public interface Listener {
        void accept(ServerPlayerEntity player, int chunkX, int chunkZ);
    }
}
