package rocks.blackblock.perf;

import net.fabricmc.api.ModInitializer;
import org.jetbrains.annotations.ApiStatus;
import rocks.blackblock.bib.BibMod;
import rocks.blackblock.bib.bv.parameter.MapParameter;
import rocks.blackblock.perf.commands.PerfCommands;
import rocks.blackblock.perf.debug.PerfDebug;
import rocks.blackblock.perf.spawn.DynamicSpawns;
import rocks.blackblock.perf.thread.DynamicThreads;

/**
 * The initializer class
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class BlackblockPerf implements ModInitializer {

    // The unique identifier of the mod
    public static final String MOD_ID = "blackblock-perf";

    // The main tweaks map, maps to `/blackblock perf`
    public static final MapParameter<?> PERF_TWEAKS = BibMod.GLOBAL_TWEAKS.add(new MapParameter<>("perf"));

    /**
     * Create a new Tweak map
     * @since    0.1.0
     */
    @ApiStatus.Internal
    public static MapParameter<?> createTweakMap(String name) {
        return PERF_TWEAKS.add(new MapParameter<>(name));
    }

    /**
     * The mod is being initialized
     * @since    0.1.0
     */
    @Override
    public void onInitialize() {
        DynamicThreads.init();
        DynamicSpawns.init();
        PerfDebug.init();
        PerfCommands.init();
    }

}
