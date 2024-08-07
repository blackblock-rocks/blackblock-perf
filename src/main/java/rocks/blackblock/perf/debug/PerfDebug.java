package rocks.blackblock.perf.debug;

import rocks.blackblock.bib.bv.parameter.IntegerParameter;
import rocks.blackblock.bib.bv.parameter.MapParameter;
import rocks.blackblock.bib.bv.value.BvInteger;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.perf.BlackblockPerf;

/**
 * Debug settings
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class PerfDebug {

    // The debug tweaks map, maps to `/blackblock perf debug`
    public static final MapParameter<?> DEBUG_TWEAKS = BlackblockPerf.createTweakMap("debug");

    // Add this amount to the MSPT
    private static final IntegerParameter MSPT_ADDITION_PARAMETER = DEBUG_TWEAKS.add(new IntegerParameter("mspt_addition"));

    // How much time to add to the MSPT
    public static long MSPT_ADDITION = 0;

    /**
     * Initialize the settings
     * @since    0.1.0
     */
    public static void init() {

        // Don't use threads by default
        MSPT_ADDITION_PARAMETER.setDefaultValue(BvInteger.of(0));

        // Listen to changes to the addition count
        MSPT_ADDITION_PARAMETER.addChangeListener(bvIntegerChangeContext -> {
            MSPT_ADDITION = (long) bvIntegerChangeContext.getValue().getFlooredInteger();

            if (MSPT_ADDITION > 0) {
                BibLog.attention("Warning! MSPT has been artificially increased by " + MSPT_ADDITION + "ms");
            }
        });
    }
}
