package rocks.blackblock.perf.thread;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.ThreadExecutor;

import java.util.function.BooleanSupplier;

public class OffloadedThreadExecutor extends ThreadExecutor<Runnable> {

    protected OffloadedThreadExecutor(String name) {
        super(name);
    }

    @Override
    public void runTasks(BooleanSupplier stopCondition) {
        super.runTasks(() -> MinecraftServer.checkWorldGenException() && stopCondition.getAsBoolean());
    }

    @Override
    public Runnable createTask(Runnable runnable) {
        return runnable;
    }

    @Override
    protected boolean canExecute(Runnable task) {
        return true;
    }

    @Override
    protected boolean shouldExecuteAsync() {
        return true;
    }

    @Override
    public Thread getThread() {
        return Thread.currentThread();
    }
}
