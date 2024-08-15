package rocks.blackblock.perf.mixin.player_watching.optimize_nearby_entity_tracking_lookups;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rocks.blackblock.perf.interfaces.player_watching.TrackingPositionInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixinForTrackingUpdates extends PlayerEntity implements TrackingPositionInfo {

    public ServerPlayerEntityMixinForTrackingUpdates(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique
    private double bb$tracking_prev_x = Double.NaN;

    @Unique
    private double bb$tracking_prev_y = Double.NaN;

    @Unique
    private double bb$tracking_prev_z = Double.NaN;

    @Override
    @Unique
    public boolean bb$isTrackingPositionUpdated() {
        final Vec3d pos = this.getPos();
        return pos.x != this.bb$tracking_prev_x || pos.y != this.bb$tracking_prev_y || pos.z != this.bb$tracking_prev_z;
    }

    @Override
    @Unique
    public void bb$updateTrackingPosition() {
        final Vec3d pos = this.getPos();
        this.bb$tracking_prev_x = pos.x;
        this.bb$tracking_prev_y = pos.y;
        this.bb$tracking_prev_z = pos.z;
    }
}
