package rocks.blackblock.perf.dynamic;

import net.minecraft.world.World;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.bib.util.BibPerf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/**
 * A dynamic setting per world.
 * Will update based on the world's performance info.
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class DynamicSetting {

    // The title of this setting
    public String title;

    // The minimum value
    private int min;

    // The maximum value, anything higher than this is not allowed
    private int max;

    // The default value, what will be used when the world is running normally
    private int default_value;

    // The value formatter
    IntFunction<String> value_formatter;

    // What to do when the value changes
    BiConsumer<World, Integer> on_change;

    // The value per world
    private final ConcurrentHashMap<World, Integer> values = new ConcurrentHashMap<>();

    // All the dynamic settings
    private static final List<DynamicSetting> SETTINGS = new ArrayList<>();

    /**
     * Initializes a new  dynamic setting
     * @since 0.1.0
     */
    public DynamicSetting(String title, int min, int max, int default_value, IntFunction<String> value_formatter, BiConsumer<World, Integer> on_change) {
        this.title = title;
        this.min = min;
        this.max = max;
        this.default_value = default_value;
        this.value_formatter = value_formatter;
        this.on_change = on_change;

        SETTINGS.add(this);
    }

    /**
     * Set the new minimum value
     * @since 0.1.0
     */
    public void setMin(int min) {
        this.min = min;
    }

    /**
     * Get the formatted value
     * @since 0.1.0
     */
    public String getFormattedValue(World world) {

        if (world == null) {
            return "null";
        }

        Integer value = this.values.get(world);

        if (value == null) {
            value = this.default_value;
        }

        if (this.value_formatter == null) {
            return Integer.toString(value);
        }

        return this.value_formatter.apply(value);
    }

    /**
     * Update based on the world's performance info
     * @since 0.1.0
     */
    public void update(BibPerf.Info info) {

        World world = info.getWorld();

        if (world == null) {
            return;
        }

        int new_value;

        if (info.isIdle() || info.isNormal()) {
            new_value = this.default_value;
        } else if (info.isOverloaded()) {
            new_value = this.min;
        } else {

            float mspt = info.getMspt();
            double load_percentage = (double)(mspt - 35) / (50 - 35);

            new_value = (int)(this.max - (this.max - this.min) * load_percentage);
        }

        Integer old_value = this.values.get(world);

        if (old_value == null || old_value != new_value) {
            this.values.put(world, new_value);
        }

        if (this.on_change != null) {
            this.on_change.accept(world, new_value);
        }
    }

    /**
     * Update all the dynamic settings
     * @since 0.1.0
     */
    public static void updateAll(BibPerf.Info info) {
        for (DynamicSetting setting : SETTINGS) {
            setting.update(info);
        }
    }
}
