package rocks.blackblock.perf.mixin.ticket_propagation;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.minecraft.server.world.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.bib.monitor.GlitchGuru;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.perf.distance.Delayed8WayDistancePropagator2D;
import rocks.blackblock.perf.thread.DynamicThreads;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Make the ChunkTicketManager use the new {@link Delayed8WayDistancePropagator2D}
 * class for propagating distances.
 *
 * @author   PaperMC team
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ChunkTicketManager.class)
public abstract class ChunkTicketManagerMixinForPropagation {

    @Mutable
    @Shadow
    @Final
    private ChunkTicketManager.TicketDistanceLevelPropagator distanceFromTicketTracker;

    @Shadow
    @Nullable
    protected abstract ChunkHolder getChunkHolder(long pos);

    @Shadow
    @Nullable
    protected abstract ChunkHolder setLevel(long pos, int level, @Nullable ChunkHolder holder, int i);

    @Shadow
    @Final
    Executor mainThreadExecutor;

    @Unique
    protected Long2IntLinkedOpenHashMap ticketLevelUpdates;

    @Unique
    protected Delayed8WayDistancePropagator2D ticketLevelPropagator;

    @Unique
    private ObjectArrayFIFOQueue<ChunkHolder> pendingChunkHolderUpdates;

    /**
     * Paper distance map propagates level from MAX to 0,
     * while vanilla propagates from 0 to MAX.
     * So there need a conversion between these values.
     * @since    0.1.0
     */
    @Unique
    private static int bb$convertBetweenTicketLevels(final int level) {
        return ChunkLevels.INACCESSIBLE - level + 1;
    }

    @Unique
    protected final void bb$updateTicketLevel(final long coordinate, final int ticketLevel) {
        if (ticketLevel > ChunkLevels.INACCESSIBLE) {
            this.ticketLevelPropagator.removeSource(coordinate);
        } else {
            this.ticketLevelPropagator.setSource(coordinate, bb$convertBetweenTicketLevels(ticketLevel));
        }
    }

    /**
     * Initialize the new ticket level propagator on init
     * @since    0.1.0
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(Executor workerExecutor, Executor mainThreadExecutor, CallbackInfo ci) {

        // fail-fast incompatibility
        this.distanceFromTicketTracker = null;

        this.ticketLevelUpdates = new Long2IntLinkedOpenHashMap() {
            @Override
            protected void rehash(int newN) {
                if (newN < this.n) {
                    return;
                }
                super.rehash(newN);
            }
        };

        this.ticketLevelPropagator = new Delayed8WayDistancePropagator2D(
                (long coordinate, byte oldLevel, byte newLevel) -> {
                    this.ticketLevelUpdates.putAndMoveToLast(coordinate, bb$convertBetweenTicketLevels(newLevel));
                }
        );

        this.pendingChunkHolderUpdates = new ObjectArrayFIFOQueue<>();
    }

    @Redirect(
        method = {
            "purge",
            "addTicket(JLnet/minecraft/server/world/ChunkTicket;)V",
            "removeTicket(JLnet/minecraft/server/world/ChunkTicket;)V",
            "removePersistentTickets"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ChunkTicketManager$TicketDistanceLevelPropagator;updateLevel(JIZ)V"
        ),
        require = 4,
        expect = 4
    )
    private void redirectUpdate(ChunkTicketManager.TicketDistanceLevelPropagator instance, long long_pos, int level, boolean decrease) {
        this.bb$updateTicketLevel(long_pos, level);
    }

    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ChunkTicketManager$TicketDistanceLevelPropagator;update(I)I"
        )
    )
    public int tickTickets(ChunkTicketManager.TicketDistanceLevelPropagator __, int distance, ServerChunkLoadingManager loading_manager) {

        if (!loading_manager.mainThreadExecutor.isOnThread()) {
            try {
                BibLog.log("Ticking tickets asynchronously on thread", Thread.currentThread().getName(), "instead of", loading_manager.mainThreadExecutor.getName());
                return DynamicThreads.SERIAL_EXECUTOR.submit(() -> {
                    BibLog.log("Delayed ticking of tickets...");
                    return this.bb$forceTickTickets(__, distance, loading_manager);
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                GlitchGuru.registerThrowable(e, "Ticket propagation");
                throw new RuntimeException(e);
            }
            //throw new ConcurrentModificationException("Attempted to tick tickets asynchronously on thread " + Thread.currentThread().getName() + " instead of " + loading_manager.mainThreadExecutor.getName());
        }

        return this.bb$forceTickTickets(__, distance, loading_manager);
    }

    @Unique
    private int bb$forceTickTickets(ChunkTicketManager.TicketDistanceLevelPropagator __, int distance, ServerChunkLoadingManager loading_manager) {

        boolean has_updates = this.ticketLevelPropagator.propagateUpdates();

        while (!this.ticketLevelUpdates.isEmpty()) {
            has_updates = true;

            long key = this.ticketLevelUpdates.firstLongKey();
            int newLevel = this.ticketLevelUpdates.removeFirstInt();

            ChunkHolder holder = this.getChunkHolder(key);
            int currentLevel = holder == null ? ChunkLevels.INACCESSIBLE + 1 : holder.getLevel();
            if (newLevel == currentLevel) continue;

            holder = this.setLevel(key, newLevel, holder, currentLevel);

            if (holder == null) {
                if (newLevel <= ChunkLevels.INACCESSIBLE) {
                    throw new IllegalStateException("Chunk holder not created");
                }
                continue;
            }

            this.pendingChunkHolderUpdates.enqueue(holder);
        }

        ArrayList<ChunkHolder> pending = new ArrayList<>(this.pendingChunkHolderUpdates.size());
        while (!this.pendingChunkHolderUpdates.isEmpty()) {
            pending.add(this.pendingChunkHolderUpdates.dequeue());
        }
        this.pendingChunkHolderUpdates.clear();
        for (ChunkHolder element : pending) {
            element.updateStatus(loading_manager);
        }
        for (ChunkHolder element : pending) {
            element.updateFutures(loading_manager, this.mainThreadExecutor);
        }

        return has_updates ? distance - 1 : distance;
    }
}
