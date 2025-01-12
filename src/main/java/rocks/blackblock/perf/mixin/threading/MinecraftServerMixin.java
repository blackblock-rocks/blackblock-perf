package rocks.blackblock.perf.mixin.threading;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.perf.debug.PerfDebug;
import rocks.blackblock.perf.dynamic.DynamicSetting;
import rocks.blackblock.perf.thread.DynamicThreads;
import rocks.blackblock.perf.util.CrashInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

/**
 * Make the servers actually use threads.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(value = MinecraftServer.class, priority = 1010)
public abstract class MinecraftServerMixin {

    @Shadow
    public abstract Iterable<ServerWorld> getWorlds();

    @Shadow
    private PlayerManager playerManager;

    /**
     * Returns an empty iterator to stop {@code MinecraftServer#tickWorlds}
     * from ticking dimensions.
     * @since    0.1.0
     */
    @WrapOperation(
        method = "tickWorlds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;getWorlds()Ljava/lang/Iterable;"
        )
    )
    public Iterable<ServerWorld> onTickWorldsGetWorlds(MinecraftServer instance, Operation<Iterable<ServerWorld>> original) {

        if (!DynamicThreads.THREADS_ENABLED) {
            return original.call(instance);
        }

        return Collections.emptyList();
    }

    /**
     * Distributes world ticking over the available worker threads.
     * Wait until they are all complete.
     * @since    0.1.0
     */
    @Inject(
        method = "tickWorlds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;getWorlds()Ljava/lang/Iterable;"
        )
    )
    public void onTickWorlds(BooleanSupplier should_keep_ticking, CallbackInfo ci) {

        if (!DynamicThreads.THREADS_ENABLED) {
            return;
        }

        // In case we run into any crashes, we'll store the info here
        AtomicReference<CrashInfo> crash_while_threaded = new AtomicReference<>();

        // Get all the worlds to tick
        Iterable<ServerWorld> worlds = this.getWorlds();

        // All the worlds start at the same time
        long start = System.currentTimeMillis() - PerfDebug.MSPT_ADDITION;

        // All the worlds that have finished by now
        Set<ServerWorld> finished_worlds = new HashSet<>();

        // Tick all the worlds on the thread pool
        DynamicThreads.THREAD_POOL.execute(worlds, world -> {

            DynamicThreads.attachToThread(Thread.currentThread(), world);

            if (BibPerf.ON_FULL_SECOND) {
                WorldTimeUpdateS2CPacket packet = new WorldTimeUpdateS2CPacket(
                        world.getTime(),
                        world.getTimeOfDay(),
                        world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)
                );
                this.playerManager.sendToDimension(packet, world.getRegistryKey());
            }

            DynamicThreads.swapThreadsAndRun(() -> {

                try {
                    world.tick(should_keep_ticking);
                } catch (Throwable throwable) {
                    crash_while_threaded.set(new CrashInfo(world, throwable));
                }

            }, world, world.getChunkManager());

            DynamicThreads.detachThread(Thread.currentThread());

            long duration = System.currentTimeMillis() - start;

            BibPerf.Info info = world.bb$getPerformanceInfo();
            info.aggregateMspt(duration);

            if (BibPerf.ON_FULL_SECOND) {
                DynamicSetting.updateAll(info);
            }

            finished_worlds.add(world);
        });

        int wait_count = 0;

        // We're going to run tasks that are meant to be run on the
        // individual world threads (minecraft:overworld etc) on the
        // main server thread.
        // This shouldn't be a problem: those world threads are actually
        // waiting on us
        while (DynamicThreads.THREAD_POOL.isActive()) {
            DynamicThreads.drainOurLocalQueue();
            DynamicThreads.THREAD_POOL.drainInactiveThreadQueues();

            // Wait 1ms before checking again
            LockSupport.parkNanos("waiting for tasks", 1_000_000L);
            wait_count++;

            // If we're waiting for more than 1 second on a single tick,
            // something is probably wrong
            if (wait_count % 1000 == 0) {
                BibLog.log("Ticking worlds is taking a very long time, still waiting for:");

                for (ServerWorld world : worlds) {
                    if (!finished_worlds.contains(world)) {
                        BibLog.log(" - " + world.getRegistryKey().getValue());
                    }
                }
            }
        }

        DynamicThreads.THREAD_POOL.awaitCompletion();

        var crash = crash_while_threaded.get();

        if (crash == null) {
            return;
        }

        crash.crash("Exception ticking world (asynchronously)");
    }

    /**
     * Shutdown all threadpools when the server stops.
     * Prevent server hang when stopping the server.
     */
    @Inject(method = "stop", at = @At("HEAD"))
    public void shutdownThreadpool(CallbackInfo ci) {

        if (!DynamicThreads.THREADS_ENABLED) {
            return;
        }

        DynamicThreads.THREAD_POOL.shutdown();
    }
}
