package rocks.blackblock.perf.mixin.chunk_ticking.broadcast;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rocks.blackblock.perf.interfaces.chunk.BroadcastRequester;

import java.util.List;
import java.util.function.Consumer;

@Mixin(value = ServerChunkManager.class, priority = 900)
public class ServerChunkManagerForBroadcasts implements BroadcastRequester {

    @Unique
    private final ReferenceLinkedOpenHashSet<ChunkHolder> bb$requires_broadcast = new ReferenceLinkedOpenHashSet<>(128);

    @Redirect(
            method = "tickChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
                    ordinal = 0
            )
    )
    private void bb$broadcastChanges(List<ServerChunkManager.ChunkWithHolder> list, Consumer<ServerChunkManager.ChunkWithHolder> consumer) {
        for (ChunkHolder holder : this.bb$requires_broadcast) {
            WorldChunk chunk = holder.getWorldChunk();
            if (chunk != null) {
                holder.flushUpdates(chunk);
            }
        }
        this.bb$requires_broadcast.clear();
    }

    @Override
    @Unique
    public void bb$requiresBroadcast(ChunkHolder holder) {
        this.bb$requires_broadcast.add(holder);
    }
}
