package rocks.blackblock.perf;

import net.fabricmc.api.ModInitializer;
import rocks.blackblock.bib.BibMod;
import rocks.blackblock.bib.bv.parameter.IntegerParameter;
import rocks.blackblock.bib.bv.parameter.MapParameter;
import rocks.blackblock.bib.bv.value.BvInteger;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.perf.thread.BlackblockThreads;
import rocks.blackblock.perf.thread.ThreadPool;

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

    // The TweakParameter to use for setting the amount of threads
    private static final IntegerParameter THREADS_PARAMETER = PERF_TWEAKS.add(new IntegerParameter("dimension_threads"));

    /**
     * The mod is being initialized
     * @since    0.1.0
     */
    @Override
    public void onInitialize() {

        // Don't use threads by default
        THREADS_PARAMETER.setDefaultValue(BvInteger.of(0));

        // Listen to changes to the thread count
        THREADS_PARAMETER.addChangeListener(bvIntegerChangeContext -> {
            BlackblockThreads.THREADS_COUNT = bvIntegerChangeContext.getValue().getFlooredInteger();
            BlackblockThreads.THREADS_ENABLED = BlackblockThreads.THREADS_COUNT > 0;

            BibLog.attention("Dimensional thread count:", BlackblockThreads.THREADS_COUNT);

            if (BlackblockThreads.THREADS_ENABLED) {
                BlackblockThreads.THREAD_POOL = new ThreadPool(BlackblockThreads.THREADS_COUNT);
            }
        });
    }

}
