package rocks.blackblock.perf.distance;

import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.perf.dynamic.DynamicSetting;

/**
 * Dynamic distances for view & simulation distance
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class DynamicDistances {

    // The dynamic simulation distance setting
    public static DynamicSetting SIMULATION_DISTANCE = new DynamicSetting(
            "Simulation Distance",
            BibPerf.State.BUSY,
            2,
            256,
            value -> value + " chunks",
            DynamicDistances::updateSimulationDistance
    );

    // The dynamic view distance setting
    public static DynamicSetting VIEW_DISTANCE = new DynamicSetting(
            "View Distance",
            BibPerf.State.OVERLOADED,
            2,
            256,
            value -> value + " chunks",
            DynamicDistances::updateViewDistance
    );

    /**
     * Initialize the settings
     * @since    0.1.0
     */
    @ApiStatus.Internal
    public static void init() {
        SIMULATION_DISTANCE.setPreferredValue(11);
        SIMULATION_DISTANCE.setPerformanceValue(4);
        SIMULATION_DISTANCE.setSmoothing(0.05f);
        SIMULATION_DISTANCE.setRollingAverageWindow(10);

        VIEW_DISTANCE.setPreferredValue(16);
        VIEW_DISTANCE.setPerformanceValue(10);
        VIEW_DISTANCE.setSmoothing(0.05f);
        VIEW_DISTANCE.setRollingAverageWindow(20);
    }

    /**
     * Update the simulation distance
     * @since    0.1.0
     */
    public static void updateSimulationDistance(World world, int value) {
        world.bb$setSimulationDistance(value);
    }

    /**
     * Update the view distance
     * @since    0.1.0
     */
    public static void updateViewDistance(World world, int value) {
        world.bb$setViewDistance(value);
    }
}
