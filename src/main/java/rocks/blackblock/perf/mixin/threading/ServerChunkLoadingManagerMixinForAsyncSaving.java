package rocks.blackblock.perf.mixin.threading;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.bib.util.BibPerf;

import java.util.function.BooleanSupplier;

@Mixin(ServerChunkLoadingManager.class)
public abstract class ServerChunkLoadingManagerMixinForAsyncSaving {

    @Unique
    private boolean bb$save_is_backlogged = false;

    @Shadow
    protected abstract boolean save(ChunkHolder chunkHolder);

    @Redirect(
            method = "unloadChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectCollection;iterator()Lit/unimi/dsi/fastutil/objects/ObjectIterator;"
            )
    )
    private ObjectIterator<ChunkHolder> bb$onIterateOverChunkHolders(ObjectCollection<ChunkHolder> instance, @Local BooleanSupplier keep_going) {

        // Throttle saving chunks less when we're backlogged
        if (this.bb$save_is_backlogged) {
            if (BibPerf.ON_EVEN_TICK) {
                return ObjectIterators.emptyIterator();
            }
        } else {
            // Only save active chunks every second
            if (!BibPerf.ON_FULL_SECOND) {
                return ObjectIterators.emptyIterator();
            }
        }

        if (instance.isEmpty()) {
            return ObjectIterators.emptyIterator();
        }

        ObjectIterator<ChunkHolder> iterator = instance.iterator();
        int save_count = 0;

        while(save_count < 20 && keep_going.getAsBoolean() && iterator.hasNext()) {
            if (this.save(iterator.next())) {
                ++save_count;
            }
        }

        if (save_count > 0) {
            if (save_count == 20) {
                this.bb$save_is_backlogged = true;
            }
        } else {
            this.bb$save_is_backlogged = false;
        }

        return ObjectIterators.emptyIterator();
    }
}
