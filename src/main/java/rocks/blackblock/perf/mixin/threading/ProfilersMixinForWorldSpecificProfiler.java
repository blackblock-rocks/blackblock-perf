package rocks.blackblock.perf.mixin.threading;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.bib.util.BibServer;

/**
 * Look for the world-specific profiler
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.2.0
 */
@Mixin(Profilers.class)
public class ProfilersMixinForWorldSpecificProfiler {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void onGet(CallbackInfoReturnable<Profiler> cir) {

        var server = BibServer.getServer();

        if (server == null) {
            return;
        }

        Thread current_thread = Thread.currentThread();

        for (ServerWorld world : server.getWorlds()) {

            if (current_thread == world.bb$getMainThread()) {
                cir.setReturnValue(world.bb$getWorldProfiler());
                return;
            }
        }
    }
}
