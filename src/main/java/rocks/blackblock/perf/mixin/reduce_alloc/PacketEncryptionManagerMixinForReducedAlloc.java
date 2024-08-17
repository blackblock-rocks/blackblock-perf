package rocks.blackblock.perf.mixin.reduce_alloc;

import net.minecraft.network.encryption.PacketEncryptionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rocks.blackblock.perf.dedup.Constants;

@Mixin(PacketEncryptionManager.class)
public class PacketEncryptionManagerMixinForReducedAlloc {
    @Shadow
    private byte[] encryptionBuffer = Constants.emptyByteArray;

    @Shadow
    private byte[] conversionBuffer = Constants.emptyByteArray;
}
