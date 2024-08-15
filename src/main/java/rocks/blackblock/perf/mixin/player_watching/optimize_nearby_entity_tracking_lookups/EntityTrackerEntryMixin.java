package rocks.blackblock.perf.mixin.player_watching.optimize_nearby_entity_tracking_lookups;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rocks.blackblock.perf.interfaces.player_watching.OptimizedEntityTrackerEntry;

@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin implements OptimizedEntityTrackerEntry {

    @Shadow
    private int trackingTick;

    @Shadow
    @Final
    private int tickInterval;

    @Shadow
    public abstract void tick();

    @Shadow
    protected abstract void syncEntityData();

    @Shadow
    @Final
    private Entity entity;

    @Shadow
    private int updatesWithoutVehicle;

    @Override
    public void bb$tickAlways() {
        this.trackingTick = MathHelper.roundUpToMultiple(this.trackingTick, this.tickInterval);
        this.updatesWithoutVehicle = 1 << 16;
        this.entity.velocityDirty = true;
        this.tick();
    }

    @Override
    public void bb$syncEntityData() {
        this.trackingTick++;
        if (this.trackingTick % this.tickInterval == 0) {
            this.syncEntityData();
        }
    }
}
