package org.geysermc.hydraulic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.compat.NativePacketBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerCommonPacketListenerImpl.class)
public class NativePacketsMixin {
    @WrapOperation(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"))
    private void hydraulic$bridge(Connection connection, Packet<?> packet, ChannelFutureListener listener, boolean flush, Operation<Void> original) {
        if ((Object) this instanceof ServerGamePacketListenerImpl game && GeyserApi.api().connectionByUuid(game.player.getUUID()) instanceof GeyserSession session) {
            packet = NativePacketBridge.remap(packet, game.player, session);
        }
        if (packet != null) original.call(connection, packet, listener, flush);
    }
}
