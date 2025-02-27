package rocks.blackblock.perf.mixin.reduce_alloc;

import net.minecraft.server.ServerConfigList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import rocks.blackblock.perf.dedup.Constants;

import java.util.Map;

@Mixin(ServerConfigList.class)
public class ServerConfigListMixinForReducedAlloc<V> {

    @Shadow
    @Final
    private Map<String, V> map;

    /**
     * @author QPCrummer
     * @reason Reduce Allocations
     */
    @Overwrite
    public String[] getNames() {
        return this.map.keySet().toArray(Constants.emptyStringArray);
    }
}
