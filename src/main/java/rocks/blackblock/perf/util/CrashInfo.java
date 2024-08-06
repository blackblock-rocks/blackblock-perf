package rocks.blackblock.perf.util;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import rocks.blackblock.bib.monitor.GlitchGuru;

public record CrashInfo(ServerWorld world, Throwable throwable) {

    public void crash(String title) {
        CrashReport report = CrashReport.create(this.throwable, title);
        this.world.addDetailsToCrashReport(report);
        throw new CrashException(report);
    }

    public void report(String title) {
        GlitchGuru.registerThrowable(this.throwable, title);
    }
}