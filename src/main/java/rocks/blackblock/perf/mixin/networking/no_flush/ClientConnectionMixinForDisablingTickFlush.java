package rocks.blackblock.perf.mixin.networking.no_flush;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.listener.PacketListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.bib.util.BibFlow;
import rocks.blackblock.bib.util.BibLog;

import java.util.Queue;
import java.util.function.Consumer;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixinForDisablingTickFlush {

    @Shadow
    private Channel channel;
    
    @Shadow
    @Final private Queue<Consumer<ClientConnection>> queuedTasks;

    @Shadow
    public abstract boolean isChannelAbsent();

    @Shadow
    public abstract boolean isOpen();

    @Unique
    private volatile boolean isClosing = false;

    private static int COUNTER = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(NetworkSide side, CallbackInfo ci) {
        var my_id = COUNTER++;
        var tasks = this.queuedTasks;

        /*
        BibLog.log("Created ClientConnection", my_id);
        BibLog.printStackTrace();
        BibFlow.onIntervalWhileReferenced(() -> {
            BibLog.log("Queued size of connection", my_id, ":", tasks.size(), "Closing?", this.isClosing,, "Is open?", this.isOpen(), "Has channels?", !this.isChannelAbsent());
        }, this, 10000);
        */
    }

    /**
     * Don't flush the channel
     */
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/channel/Channel;flush()Lio/netty/channel/Channel;"
        )
    )
    private Channel dontFlush(Channel instance) {
        return instance; // no-op
    }

    /**
     * Mark as closing on disconnect
     */
    @Redirect(
        method = "disconnect(Lnet/minecraft/network/DisconnectionInfo;)V",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;",
            remap = false
        )
    )
    private ChannelFuture noDisconnectWait(ChannelFuture instance) {
        this.isClosing = true;
        return instance;
    }

    /**
     * Make sure the isOpen() call returns false
     * when a close is in progress
     */
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/channel/Channel;isOpen()Z",
            remap = false
        )
    )
    private boolean redirectIsOpen(Channel instance) {
        return this.channel != null && (this.channel.isOpen() && !this.isClosing);
    }
}
