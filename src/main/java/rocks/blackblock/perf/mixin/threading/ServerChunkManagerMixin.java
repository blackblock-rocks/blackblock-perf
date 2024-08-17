package rocks.blackblock.perf.mixin.threading;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.server.world.*;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.SpawnDensityCapper;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.perf.interfaces.chunk.BroadcastRequester;
import rocks.blackblock.perf.thread.DynamicThreads;
import rocks.blackblock.perf.thread.WithMutableThread;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = ServerChunkManager.class, priority = 1001)
public abstract class ServerChunkManagerMixin extends ChunkManager implements WithMutableThread, BroadcastRequester {

    @Unique
    private final ReferenceLinkedOpenHashSet<ChunkHolder> bb$requires_broadcast = new ReferenceLinkedOpenHashSet<>(128);

    @Unique
    private boolean bb$do_mob_spawning = true;

    @Unique
    private int bb$random_tick_speed = 4;

    @Shadow
    Thread serverThread;

    @Shadow
    @Final
    ServerWorld world;

    @Shadow
    private long lastMobSpawningTime;

    @Shadow
    @Final
    public ChunkTicketManager ticketManager;

    @Shadow
    @Nullable
    private SpawnHelper.@Nullable Info spawnInfo;

    @Shadow
    private boolean spawnMonsters;

    @Shadow
    private boolean spawnAnimals;

    @Shadow
    @Final
    public ServerChunkLoadingManager chunkLoadingManager;

    @Shadow
    protected abstract void ifChunkLoaded(long pos, Consumer<WorldChunk> chunkConsumer);

    @Override
    @Unique
    public Thread bb$getMainThread() {
        return this.serverThread;
    }

    @Override
    @Unique
    public void bb$setMainThread(Thread thread) {
        this.serverThread = thread;
    }

    @WrapOperation(
        method = "getChunk",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"
        )
    )
    public Thread currentThread(Operation<Thread> original) {

        Thread thread = original.call();

        if (DynamicThreads.THREADS_ENABLED && DynamicThreads.ownsThread(thread)) {
            return this.serverThread;
        }

        return thread;
    }

    /**
     * We basically replace the entire tickChunks method,
     * but we don't use an `@Overwrite` to keep some mods from throwing errors
     *
     * @since 0.1.0
     */
    @Inject(method = "tickChunks", at = @At("HEAD"), cancellable = true)
    private void bb$onTickChunks(CallbackInfo ci) {

        // Don't run the original method
        ci.cancel();

        if (BibPerf.ON_FULL_SECOND) {
            this.bb$do_mob_spawning = this.world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING);
            this.bb$random_tick_speed = this.world.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED);
        }

        long current_time = this.world.getTime();
        long time_since_last_spawn = current_time - this.lastMobSpawningTime;
        this.lastMobSpawningTime = current_time;

        this.world.bb$resetIceAndSnowTick();

        List<ServerChunkManager.ChunkWithHolder> loaded_chunks = bb$getTickableChunks();

        if (this.world.getTickManager().shouldTick()) {

            int ticked_chunk_count = this.ticketManager.getTickedChunkCount();
            SpawnHelper.Info spawn_info = this.bb$setupSpawnInfo(ticked_chunk_count);
            this.spawnInfo = spawn_info;

            bb$shuffleChunks(loaded_chunks);

            boolean is_time_to_spread_surface = this.world.getLevelProperties().getTime() % 400L == 0L;

            for (ServerChunkManager.ChunkWithHolder chunk_with_holder : loaded_chunks) {
                this.bb$tickChunk(chunk_with_holder, time_since_last_spawn, is_time_to_spread_surface, spawn_info);
            }

            if (this.bb$do_mob_spawning) {
                this.world.tickSpawners(this.spawnMonsters, this.spawnAnimals);
            }
        }

        bb$broadcastChunkUpdates(loaded_chunks);
    }

    /**
     * Get all the loaded chunks that can be ticked
     * @since 0.1.0
     */
    @Unique
    private List<ServerChunkManager.ChunkWithHolder> bb$getTickableChunks() {

        var tickable_chunk_map = this.chunkLoadingManager.bb$tickableChunkMap();

        List<ServerChunkManager.ChunkWithHolder> chunks = new ArrayList<>(tickable_chunk_map.size());

        for (ChunkHolder holder : tickable_chunk_map.values()) {
            WorldChunk chunk = holder.getWorldChunk();
            if (chunk != null) {
                chunks.add(new ServerChunkManager.ChunkWithHolder(chunk, holder));
            }
        }
        return chunks;
    }

    /**
     * Setup the spawn info for the current tick
     * @since 0.1.0
     */
    @Unique
    private SpawnHelper.Info bb$setupSpawnInfo(int ticked_chunk_count) {
        return SpawnHelper.setupSpawn(ticked_chunk_count, this.world.iterateEntities(), this::ifChunkLoaded, new SpawnDensityCapper(this.chunkLoadingManager));
    }

    /**
     * Shuffle the chunks every third tick
     * @since 0.1.0
     */
    @Unique
    private void bb$shuffleChunks(List<ServerChunkManager.ChunkWithHolder> chunks) {
        if (BibPerf.ON_THIRD_TICK) {
            Util.shuffle(chunks, this.world.random);
        }
    }

    /**
     * Actually tick the given chunk
     * @since 0.1.0
     */
    @Unique
    private void bb$tickChunk(ServerChunkManager.ChunkWithHolder chunk_with_holder, long time_since_last_spawn, boolean is_time_to_spread_surface, SpawnHelper.Info spawn_info) {
        WorldChunk chunk = chunk_with_holder.chunk();
        ChunkPos chunk_pos = chunk.getPos();
        if (!this.world.shouldTick(chunk_pos) || !this.chunkLoadingManager.shouldTick(chunk_pos)) {
            return;
        }

        chunk.increaseInhabitedTime(time_since_last_spawn);
        if (this.bb$do_mob_spawning && (this.spawnMonsters || this.spawnAnimals) && this.world.getWorldBorder().contains(chunk_pos)) {
            SpawnHelper.spawn(this.world, chunk, spawn_info, this.spawnAnimals, this.spawnMonsters, is_time_to_spread_surface);
        }

        if (this.world.shouldTickBlocksInChunk(chunk_pos.toLong())) {
            this.world.tickChunk(chunk, this.bb$random_tick_speed);
        }
    }

    /**
     * Broadcast chunk updates to all players
     * @since 0.1.0
     */
    @Unique
    private void bb$broadcastChunkUpdates(List<ServerChunkManager.ChunkWithHolder> chunks) {
        chunks.forEach(chunk -> chunk.holder().flushUpdates(chunk.chunk()));
        for (ChunkHolder holder : this.bb$requires_broadcast) {
            WorldChunk chunk = holder.getWorldChunk();
            if (chunk != null) {
                holder.flushUpdates(chunk);
            }
        }
        this.bb$requires_broadcast.clear();
    }

    /**
     * Make the given chunk holder require a broadcast
     * @since 0.1.0
     */
    @Override
    @Unique
    public void bb$requiresBroadcast(ChunkHolder holder) {
        this.bb$requires_broadcast.add(holder);
    }
}
