package rocks.blackblock.perf.mixin.compact_bit_storage;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.world.chunk.PalettedContainer;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Optimizes memory usage of PalettedContainers by compressing oversized, homogeneous chunks.
 *
 * This mixin detects when a chunk is using more bits than necessary to store its data,
 * particularly when all entries are the same. It then recreates the data structure
 * using the most compact representation possible, significantly reducing memory footprint
 * for uniform chunks in large Minecraft worlds.
 *
 * @author   embeddedt
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(PalettedContainer.class)
public abstract class PalettedContainerMixin<T> {

    @Shadow
    private volatile PalettedContainer.Data<T> data;

    @Shadow
    protected abstract PalettedContainer.Data<T> getCompatibleData(@Nullable PalettedContainer.Data<T> previousData, int bits);

    @Inject(
        method = "readPacket",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/chunk/PalettedContainer;data:Lnet/minecraft/world/chunk/PalettedContainer$Data;",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void validateData(PacketByteBuf buf, CallbackInfo ci, int i) {

        if (i <= 1) {
            return;
        }

        long[] storArray = this.data.storage().getData();
        boolean empty = true;
        for (long l : storArray) {
            if (l != 0) {
                empty = false;
                break;
            }
        }
        if (empty && storArray.length > 0) {
            // It means the chunk is oversized and wasting memory,
            // take the ID out of the palette and recreate a smaller chunk
            T value = this.data.palette().get(0);
            this.data = this.getCompatibleData(null, 0);
            this.data.palette().index(value);
        }
    }
}
