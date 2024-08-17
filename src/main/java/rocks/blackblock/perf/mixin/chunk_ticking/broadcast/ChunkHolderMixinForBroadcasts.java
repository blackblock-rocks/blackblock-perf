package rocks.blackblock.perf.mixin.chunk_ticking.broadcast;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.LightType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixinForBroadcasts {

    @Shadow
    @Final
    private HeightLimitView world;

    @Inject(
        method = "markForBlockUpdate",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/world/ChunkHolder;pendingBlockUpdates:Z",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void bb$onBlockChanged(BlockPos blockPos, CallbackInfo ci) {
        this.bb$requiresBroadcast();
    }

    @Inject(
            method = "markForLightUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/BitSet;set(I)V"
            )
    )
    private void bb$onLightChanged(LightType lightType, int i, CallbackInfo ci) {
        this.bb$requiresBroadcast();
    }

    @Unique
    private void bb$requiresBroadcast() {
        if (this.world instanceof ServerWorld server_world) {
            server_world.getChunkManager().bb$requiresBroadcast((ChunkHolder) (Object) this);
        }
    }
}
