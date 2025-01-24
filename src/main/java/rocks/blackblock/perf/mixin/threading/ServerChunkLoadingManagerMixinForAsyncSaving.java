package rocks.blackblock.perf.mixin.threading;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongIterators;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.bib.util.BibPerf;

import java.util.function.BooleanSupplier;

/**
 * Throttle saving active chunks.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(ServerChunkLoadingManager.class)
public abstract class ServerChunkLoadingManagerMixinForAsyncSaving implements BibLog.Argable {

    @Unique
    private boolean bb$save_is_backlogged = false;

    @Shadow
    protected abstract boolean save(ChunkHolder chunkHolder, long currentTime);

    @Shadow
    @Final
    public ThreadExecutor<Runnable> mainThreadExecutor;

    @Shadow
    public abstract int getLoadedChunkCount();

    @Shadow
    @Final
    public ServerWorld world;

    @Shadow private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> chunkHolders;

    @Redirect(
        method = "saveChunks",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/LongSet;iterator()Lit/unimi/dsi/fastutil/longs/LongIterator;"
        )
    )
    private LongIterator bb$onIterateOverChunkHolders(LongSet instance, @Local BooleanSupplier keep_going) {

        // Throttle saving chunks less when we're backlogged
        if (this.bb$save_is_backlogged) {
            if (BibPerf.ON_EVEN_TICK) {
                return LongIterators.EMPTY_ITERATOR;
            }
        } else {
            // Only save active chunks every second
            if (!BibPerf.ON_FULL_SECOND) {
                return LongIterators.EMPTY_ITERATOR;
            }
        }

        if (instance.isEmpty()) {
            return LongIterators.EMPTY_ITERATOR;
        }

        long now = Util.getMeasuringTimeMs();
        LongIterator iterator = instance.iterator();
        int save_count = 0;

        while(save_count < 20 && keep_going.getAsBoolean() && iterator.hasNext()) {
            long chunk_pos = iterator.nextLong();
            ChunkHolder chunkHolder = this.chunkHolders.get(chunk_pos);
            Chunk chunk = chunkHolder != null ? chunkHolder.getLatest() : null;

            if (chunk != null && chunk.needsSaving()) {
                if (this.save(chunkHolder, now)) {
                    ++save_count;
                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }

        if (save_count > 0) {
            if (save_count == 20) {
                this.bb$save_is_backlogged = true;
            }
        } else {
            this.bb$save_is_backlogged = false;
        }

        return LongIterators.EMPTY_ITERATOR;
    }

    @Unique
    @Override
    public BibLog.Arg toBBLogArg() {

        var result = BibLog.createArg(this);

        result.add("world", this.world);
        result.add("main_thread", this.mainThreadExecutor.getThread());
        result.add("loaded_chunk_count", this.getLoadedChunkCount());

        return result;
    }
}
