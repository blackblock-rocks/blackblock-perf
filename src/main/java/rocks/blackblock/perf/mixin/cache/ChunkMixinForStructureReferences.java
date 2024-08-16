package rocks.blackblock.perf.mixin.cache;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.*;

import java.util.Collections;
import java.util.Map;

@Mixin(Chunk.class)
public class ChunkMixinForStructureReferences {

    @Shadow
    @Final
    private Map<Structure, LongSet> structureReferences;

    @Unique
    private Map<?, ?> bb$cached_structure_references = null;

    /**
     * @author embeddedt
     * @reason Cache returned map view to avoid allocations, return empty map when possible
     * so that iterator() calls don't allocate
     * <p></p>
     * Note: technically, this introduces an API change, as the return value may no longer be a live view
     * of the structure references of the chunk. It's unlikely this will affect anything in practice.
     */
    @Overwrite
    public Map<?, ?> getStructureReferences() {

        if (this.structureReferences.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<?, ?> view = this.bb$cached_structure_references;

        if (view == null) {
            this.bb$cached_structure_references = view = Collections.unmodifiableMap(this.structureReferences);
        }

        return view;
    }
}
