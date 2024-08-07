package rocks.blackblock.perf.spawn;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import rocks.blackblock.bib.bv.parameter.IntegerParameter;
import rocks.blackblock.bib.bv.parameter.MapParameter;
import rocks.blackblock.bib.bv.value.BvInteger;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.perf.BlackblockPerf;
import rocks.blackblock.perf.dynamic.DynamicSetting;

/**
 * The main Dynamic Spawns class
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class DynamicSpawns {

    // The "spawning" tweak map
    public static final MapParameter<?> SPAWNING_TWEAKS = BlackblockPerf.createTweakMap("spawning");

    // The TweakParameter to use for setting the minimum modifier percentage
    private static final IntegerParameter MIN_MODIFIER_PERCENTAGE_PARAMETER = SPAWNING_TWEAKS.add(new IntegerParameter("min_modifier_percentage"));

    // The minimum modifier percentage
    public static int MIN_MODIFIER_PERCENTAGE = 30;

    // The dynamic setting
    public static DynamicSetting MOBCAP_MODIFIER = new DynamicSetting(
            "Mobcap Modifier",
            30,
            100,
            100,
            value -> value + "%",
            DynamicSpawns::updateMobcaps
    );

    /**
     * Initialize the settings
     * @since    0.1.0
     */
    @ApiStatus.Internal
    public static void init() {

        // The minimum modifier percentage is 30%
        MIN_MODIFIER_PERCENTAGE_PARAMETER.setDefaultValue(BvInteger.of(30));

        // Listen to changes to the thread count
        MIN_MODIFIER_PERCENTAGE_PARAMETER.addChangeListener(bvIntegerChangeContext -> {
            int min_modifier = bvIntegerChangeContext.getValue().getFlooredInteger();
            DynamicSpawns.setNewMinModifierPercentage(min_modifier);
        });
    }

    /**
     * Set the new minimum modifier percentage
     * @since    0.1.0
     */
    public static void setNewMinModifierPercentage(int new_min_modifier_percentage) {

        if (MIN_MODIFIER_PERCENTAGE == new_min_modifier_percentage) {
            return;
        }

        DynamicSpawns.MIN_MODIFIER_PERCENTAGE = new_min_modifier_percentage;

        BibLog.attention("Minimum modifier percentage:", DynamicSpawns.MIN_MODIFIER_PERCENTAGE);
    }

    /**
     * Update the mobcaps in the given world
     * @since    0.1.0
     */
    public static void updateMobcaps(World world, int modifier) {

        double modifier_percentage = modifier / 100.0;

        for (SpawnGroup group : SpawnGroup.values()) {
            group.bb$setCapacityModifier(world, modifier_percentage);
        }
    }

}
