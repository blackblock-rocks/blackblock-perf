package rocks.blackblock.perf.activation_range;

import net.minecraft.world.World;
import rocks.blackblock.perf.dynamic.DynamicSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Actual activation range settings
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
public class ActivationRange {

    public static final List<ActivationRange> ACTIVATION_RANGES = new ArrayList<>();

    // The name of this activation range (Like what entities it is used for)
    private String name;

    // The interval between inactive entity wakeups in seconds.
    private int inactive_wakeup_interval = -1;

    // Should this entity be woken up after a certain amount of inactive ticks?
    private boolean wakeup_after_inactive_ticks = false;

    // Allows entities to be ticked when far above the player when vertical range is in use
    private boolean allow_extra_up = false;

    // Allows entities to be ticked when far below the player when vertical range is in use
    private boolean allow_extra_down = false;

    // Allow vertical range?
    private boolean allow_vertical_range = false;

    // The extra height above the player that entities can be ticked when vertical range is in use
    private int vertical_range_extra_height = 0;

    // The extra height below the player that entities can be ticked when vertical range is in use
    private int vertical_range_extra_height_down = 0;

    // The range an entity is required to be in from a player to be activated
    private DynamicSetting dynamic_activation_range = null;

    // Even when an entity is inactive, we still want to tick it every so often
    // This setting controls how often we do that.
    // The lower the value, the more often we tick "inactive" entities.
    private DynamicSetting dynamic_active_tick_delay = null;

    /**
     * Build a new activation range
     * @since 0.1.0
     */
    public static ActivationRange.Builder build(String name) {
        return ActivationRange.Builder.create(name);
    }

    /**
     * Initializes a new activation range
     * @since 0.1.0
     */
    public ActivationRange(String name) {
        this.name = name;
        ACTIVATION_RANGES.add(this);
    }

    /**
     * Set the activation range
     * @since 0.1.0
     */
    public void setActivationRange(int min, int max) {

        if (this.dynamic_activation_range == null) {
            this.dynamic_activation_range = new DynamicSetting(this.name, min, 999, max, value -> value + "", null);
        }

        this.dynamic_activation_range.setMin(min);
        this.dynamic_activation_range.setDefaultValue(max);
    }

    /**
     * Get the current activation range
     * @since 0.1.0
     */
    public int getActivationRange(World world) {
        return this.dynamic_activation_range.getValue(world);
    }

    /**
     * Should entities be ticked when far above the player?
     * @since 0.1.0
     */
    public boolean useExtraHeightUp() {
        return this.allow_extra_up;
    }

    /**
     * Should entities be ticked when far below the player?
     * @since 0.1.0
     */
    public boolean useExtraHeightDown() {
        return this.allow_extra_down;
    }

    /**
     * Should vertical range be used?
     * @since 0.1.0
     */
    public boolean isAllowVerticalRange() {
        return this.allow_vertical_range;
    }

    /**
     * Set the vertical range extra height
     * @since 0.1.0
     */
    public void setExtraHeightUp(int extra_height) {
        this.vertical_range_extra_height = extra_height;

        if (extra_height > 0) {
            this.allow_extra_up = true;
            this.allow_vertical_range = true;
        }
    }

    /**
     * Get the vertical range extra height
     * @since 0.1.0
     */
    public int getExtraHeightUp() {
        return this.vertical_range_extra_height;
    }

    /**
     * Set the vertical range extra height down
     * @since 0.1.0
     */
    public void setExtraHeightDown(int extra_height) {
        this.vertical_range_extra_height_down = extra_height;

        if (extra_height > 0) {
            this.allow_extra_down = true;
            this.allow_vertical_range = true;
        }
    }

    /**
     * Get the vertical range extra height down
     * @since 0.1.0
     */
    public int getExtraHeightDown() {
        return this.vertical_range_extra_height_down;
    }

    /**
     * Set the active tick delay
     * @since 0.1.0
     */
    public void setActiveTickDelay(int min, int max) {

        if (this.dynamic_active_tick_delay == null) {
            this.dynamic_active_tick_delay = new DynamicSetting(this.name, min, 999, max, value -> value + "", null);
        }

        this.dynamic_active_tick_delay.setMin(min);
        this.dynamic_active_tick_delay.setDefaultValue(max);
    }

    /**
     * Get the current active tick delay
     * @since 0.1.0
     */
    public int getActiveTickDelay(World world) {
        return this.dynamic_active_tick_delay.getValue(world);
    }

    /**
     * Should this entity be woken up after a certain amount of inactive ticks?
     * @since 0.1.0
     */
    public boolean wakeupAfterInactiveTicks() {
        return this.wakeup_after_inactive_ticks;
    }

    /**
     * Set the interval between inactive entity wakeups in seconds.
     * @since 0.1.0
     */
    public void setInactiveWakeupInterval(int interval) {
        this.inactive_wakeup_interval = interval;

        if (interval > 0) {
            this.wakeup_after_inactive_ticks = true;
        } else {
            this.wakeup_after_inactive_ticks = false;
        }
    }

    /**
     * Get the interval between inactive entity wakeups in seconds.
     * @since 0.1.0
     */
    public int getInactiveWakeupInterval() {
        return this.inactive_wakeup_interval;
    }

    /**
     * Should new entities be ticked?
     * @since 0.1.0
     */
    public boolean tickNewEntities() {
        return true;
    }

    /**
     * A simple builder class
     * @since 0.1.0
     */
    public static class Builder {

        private String name;
        private int min_activation_range = 14;
        private int max_activation_range = 14;
        private int min_active_tick_delay = 18;
        private int max_active_tick_delay = 18;
        private int vertical_range_extra_height = 0;
        private int vertical_range_extra_height_down = 0;
        private int inactive_wakeup_interval = -1;

        /**
         * Create a new builder
         * @since 0.1.0
         */
        private Builder(String name) {
            this.name = name;
        }

        /**
         * Create a new builder
         * @since 0.1.0
         */
        public static Builder create(String name) {
            return new Builder(name);
        }

        /**
         * Set the activation range
         * @since 0.1.0
         */
        public Builder setActivationRange(int min, int max) {
            this.min_activation_range = min;
            this.max_activation_range = max;
            return this;
        }

        /**
         * Set the active tick delay
         * @since 0.1.0
         */
        public Builder setActiveTickDelay(int min, int max) {
            this.min_active_tick_delay = min;
            this.max_active_tick_delay = max;
            return this;
        }

        /**
         * Set the vertical range extra height
         * @since 0.1.0
         */
        public Builder setVerticalRangeExtraHeight(int extra_height) {
            this.vertical_range_extra_height = extra_height;
            return this;
        }

        /**
         * Set the vertical range extra height down
         * @since 0.1.0
         */
        public Builder setVerticalRangeExtraHeightDown(int extra_height) {
            this.vertical_range_extra_height_down = extra_height;
            return this;
        }

        /**
         * Set the interval between inactive entity wakeups in seconds.
         * @since 0.1.0
         */
        public Builder setInactiveWakeupInterval(int interval) {
            this.inactive_wakeup_interval = interval;
            return this;
        }

        /**
         * Build the activation range
         * @since 0.1.0
         */
        public ActivationRange build() {
            ActivationRange range = new ActivationRange(this.name);
            range.setActivationRange(this.min_activation_range, this.max_activation_range);
            range.setActiveTickDelay(this.min_active_tick_delay, this.max_active_tick_delay);
            range.setExtraHeightUp(this.vertical_range_extra_height);
            range.setExtraHeightDown(this.vertical_range_extra_height_down);
            range.setInactiveWakeupInterval(this.inactive_wakeup_interval);
            return range;
        }
    }
}
