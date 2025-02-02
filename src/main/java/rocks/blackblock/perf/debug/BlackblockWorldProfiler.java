package rocks.blackblock.perf.debug;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.SampleType;
import org.jetbrains.annotations.Nullable;
import rocks.blackblock.bib.util.BibLog;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

public class BlackblockWorldProfiler implements Profiler, BibLog.Argable {

    private static final long TIMEOUT_NANOSECONDS = Duration.ofMillis(100L).toNanos();
    protected final ServerWorld world;
    protected boolean tick_started = false;
    protected Long start_time = null;
    protected List<Tracker> path = Collections.synchronizedList(new ArrayList<>());
    protected boolean check_timeout = false;

    public BlackblockWorldProfiler(ServerWorld world) {
        this.world = world;
    }

    public long getDurationInNanoSeconds() {

        if (!this.tick_started) {
            return 0;
        }

        return System.nanoTime() - this.start_time;
    }

    @Nullable
    public BlackblockWorldProfiler.Tracker getLastTracker() {

        if (this.path.isEmpty()) {
            return null;
        }

        return this.path.getLast();
    }

    @Override
    public void startTick() {

        if (this.tick_started) {
            BibLog.warn("Profiler tick has already started: missing endTick()?");
            return;
        }

        this.start_time = System.nanoTime();
        this.tick_started = true;
        this.path.clear();
        this.push("root");
    }

    @Override
    public void endTick() {

        if (!this.tick_started) {
            BibLog.warn("Profiler tick has not started: missing startTick()?");
            return;
        }

        this.pop();
        this.tick_started = false;
        this.start_time = null;

        if (!this.path.isEmpty()) {
            BibLog.warn("Profiler tick has ended before path was fully popped! Mismatched push/pop?", this);
        }

    }

    @Override
    public void push(String location) {

        if (!this.tick_started) {
            BibLog.warn("Profiler tick has not started: missing startTick()?", location, this);
            return;
        }

        Tracker parent = this.getLastTracker();

        if (parent != null) {
            parent.pause();
        }

        Tracker loc = new Tracker(location, System.nanoTime());
        this.path.add(loc);
    }

    @Override
    public void push(Supplier<String> locationGetter) {
        this.push(locationGetter.get());
    }

    @Override
    public void pop() {

        if (!this.tick_started) {
            BibLog.warn("Cannot pop from profiler if profiler tick hasn't started - missing startTick()?", this);
            return;
        }

        if (this.path.isEmpty()) {
            BibLog.warn("Tried to pop one too many times! Mismatched push() and pop()?", this);
            return;
        }

        Tracker last = this.getLastTracker();

        if (this.check_timeout && last != null) {
            long timeout = last.getOwnDuration();

            if (timeout > TIMEOUT_NANOSECONDS) {
                BibLog.warn("Something is taking too long!", last, this);
            }
        }

        if (last != null) {
            last.end();
            this.path.removeLast();

            last = this.getLastTracker();

            if (last != null) {
                last.resume();
            }
        }
    }

    @Override
    public void swap(String location) {
        this.pop();
        this.push(location);
    }

    @Override
    public void swap(Supplier<String> locationGetter) {
        this.pop();
        this.push(locationGetter);
    }

    @Override
    public void markSampleType(SampleType type) {

    }

    @Override
    public void visit(String marker, int num) {
        Tracker last = this.getLastTracker();

        if (last != null) {
            last.visit(marker, num);
        }
    }

    @Override
    public void visit(Supplier<String> markerGetter, int num) {
        this.visit(markerGetter.get(), num);
    }

    @Override
    public BibLog.Arg toBBLogArg() {
        var result = BibLog.createArg(this);
        result.add("world", this.world);
        result.add("entries", this.path);
        result.add("total_duration", this.getDurationInNanoSeconds() / 1000000);
        return result;
    }

    @Override
    public String toString() {
        return this.toBBLogArg().toString();
    }

    public static class Tracker implements BibLog.Argable {

        private final String name;
        private Long original_start_time = null;
        private Long current_start_time = null;
        private Long end_time = null;
        private Map<String, Integer> visits = null;
        private long own_duration = 0;

        public Tracker(String name, Long start) {
            this.name = name;
            this.original_start_time = start;
            this.current_start_time = start;
        }

        public long getOwnDuration() {
            return this.own_duration;
        }

        public long getFullDuration() {
            if (this.end_time == null) {
                return System.nanoTime() - this.original_start_time;
            } else {
                return this.end_time - this.original_start_time;
            }
        }

        public void visit(String name, int amount) {

            if (this.visits == null) {
                this.visits = new HashMap<>();
            }

            int existing = this.visits.getOrDefault(name, 0);
            this.visits.put(name, existing + amount);
        }

        public void pause() {

            // If it is already paused, do nothing
            if (this.current_start_time == null) {
                return;
            }

            // Add the current duration
            long current_duration = System.nanoTime() - this.current_start_time;
            this.own_duration += current_duration;
            this.current_start_time = null;
        }

        public void resume() {

            if (this.current_start_time != null) {
                BibLog.warn("Location is already resumed!", this);
                return;
            }

            if (this.end_time != null) {
                BibLog.warn("Location is already ended!", this);
                return;
            }

            this.current_start_time = System.nanoTime();
        }

        public void end() {
            this.pause();
            this.end_time = System.nanoTime();
        }

        @Override
        public BibLog.Arg toBBLogArg() {

            var result = BibLog.createArg(this);
            result.add("name", this.name);
            result.add("own_duration", this.getOwnDuration() / 1000000);
            result.add("full_duration", this.getFullDuration() / 1000000);

            if (this.end_time != null) {
                result.add("ended", false);
            }

            if (this.visits != null) {
                result.add("visits", this.visits);
            }

            return result;
        }
    }
}
