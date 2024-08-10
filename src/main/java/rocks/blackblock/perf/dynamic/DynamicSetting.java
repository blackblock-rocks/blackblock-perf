package rocks.blackblock.perf.dynamic;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.bib.util.BibText;

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

    // The value to use when the world is under high load
    private int performance_value;

    // The preferred value, what will be used when the world is running normally
    private int preferred_value;

    // The lower limit of the value
    private final int lower_limit;

    // The upper limit of the value
    private final int upper_limit;

    // The minimum performance state at which the performance values should be used
    private final BibPerf.State min_performance_state;

    // The value formatter
    IntFunction<String> value_formatter;

    // What to do when the value changes
    BiConsumer<World, Integer> on_change;

    // The value per world
    private final ConcurrentHashMap<World, Integer> values = new ConcurrentHashMap<>();

    // All the dynamic settings
    private static final List<DynamicSetting> SETTINGS = new ArrayList<>();

    /**
     * Initializes a new dynamic setting
     * @since 0.1.0
     */
    public DynamicSetting(String title, BibPerf.State min_performance_state, int lower_limit, int upper_limit, IntFunction<String> value_formatter, BiConsumer<World, Integer> on_change) {

        if (lower_limit > upper_limit) {
            throw new IllegalArgumentException("lower_limit must be lower than upper_limit");
        }

        this.title = title;
        this.min_performance_state = min_performance_state;
        this.lower_limit = lower_limit;
        this.upper_limit = upper_limit;
        this.value_formatter = value_formatter;
        this.on_change = on_change;

        SETTINGS.add(this);
    }

    /**
     * Set the performance value
     * @since 0.1.0
     */
    public void setPerformanceValue(int performance_value) {

        if (performance_value < this.lower_limit || performance_value > this.upper_limit) {
            throw new IllegalArgumentException("performance_value must be between lower_limit and upper_limit");
        }

        this.performance_value = performance_value;
    }

    /**
     * Set the new default (preferred) value
     * @since 0.1.0
     */
    public void setPreferredValue(int preferred_value) {

        if (preferred_value < this.lower_limit || preferred_value > this.upper_limit) {
            throw new IllegalArgumentException("preferred_value must be between lower_limit and upper_limit");
        }

        this.preferred_value = preferred_value;
    }

    /**
     * Get the current value
     * @since 0.1.0
     */
    public int getCurrentValue(World world) {
        return this.values.getOrDefault(world, this.preferred_value);
    }

    /**
     * Get the formatted value
     * @since 0.1.0
     */
    public String getFormattedValue(World world) {

        if (world == null) {
            return "null";
        }

        Integer value = this.getCurrentValue(world);

        if (this.value_formatter == null) {
            return Integer.toString(value);
        }

        return this.value_formatter.apply(value);
    }

    /**
     * Get the value as a Text instance,
     * with colour & a tooltip
     * @since 0.1.0
     */
    public MutableText getCurrentValueText(World world) {

        int current_value = this.getCurrentValue(world);
        String value_str;

        if (this.value_formatter == null) {
            value_str = Integer.toString(current_value);
        } else {
            value_str = this.value_formatter.apply(current_value);
        }

        MutableText result = Text.literal(value_str);

        if (current_value == this.preferred_value) {
            result = result.formatted(Formatting.GREEN);
        } else {
            // Slowly fade the colour from orange to red
            int max_difference = Math.abs(this.preferred_value - this.performance_value);
            int difference = Math.abs(current_value - this.preferred_value);
            int percentage = difference * 100 / max_difference;

            if (percentage > 50) {
                result = result.formatted(Formatting.RED);
            } else if (percentage > 25) {
                result = result.formatted(Formatting.GOLD);
            } else {
                result = result.formatted(Formatting.YELLOW);
            }
        }

        BibText.Lore hover_text = BibText.createLore();
        hover_text.addLine("Current value", result.copy());
        hover_text.addLine("Preferred value", Text.literal(this.preferred_value + "").formatted(Formatting.GREEN));
        hover_text.addLine("Performance value", Text.literal(this.performance_value + "").formatted(Formatting.RED));

        HoverEvent tooltip = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover_text.get());
        result.setStyle(result.getStyle().withHoverEvent(tooltip));

        return result;
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

        BibPerf.State currentState = info.getCurrentState();
        BibPerf.State targetState = info.getTargetState();

        Integer old_value = this.values.getOrDefault(world, this.preferred_value);

        int target_value;
        if (currentState == targetState) {
            // We're in a stable state
            target_value = calculateValueForState(currentState);
        } else if (currentState.getSeverity() < targetState.getSeverity()) {
            // We're ramping up to a more severe state
            int currentStateValue = calculateValueForState(currentState);
            int targetStateValue = calculateValueForState(targetState);
            target_value = (int) lerp(currentStateValue, targetStateValue, info.getRampUpProgress());
        } else {
            // We're recovering to a less severe state
            int currentStateValue = calculateValueForState(currentState);
            int targetStateValue = calculateValueForState(targetState);
            target_value = (int) lerp(currentStateValue, targetStateValue, info.getRecoveryProgress());
        }

        // Apply smoothing
        float new_calculated_value = lerp(old_value, target_value, 0.2f);

        // Ensure the new value is within limits
        int new_value = Math.max(this.lower_limit, Math.min(this.upper_limit, Math.round(new_calculated_value)));

        if (old_value != new_value) {
            this.values.put(world, new_value);

            if (this.on_change != null) {
                this.on_change.accept(world, new_value);
            }
        }
    }

    /**
     * Calculate the value for the given state
     * @since 0.1.0
     */
    private int calculateValueForState(BibPerf.State state) {
        float state_modifier = state.getPerformanceModifier();
        return Math.round(this.preferred_value + (this.performance_value - this.preferred_value) * state_modifier);
    }

    /**
     * Linear interpolation
     * @since 0.1.0
     */
    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
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
