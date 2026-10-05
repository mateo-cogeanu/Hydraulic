package org.geysermc.hydraulic.mixin.ext;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.compat.SessionTrafficAccess;
import org.geysermc.hydraulic.compat.TrafficDiagnostics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = GeyserSession.class, remap = false)
public class SessionTrafficMixin implements SessionTrafficAccess {
    @Unique private final TrafficDiagnostics hydraulic$diagnostics = new TrafficDiagnostics();
    public TrafficDiagnostics hydraulic$traffic() { return hydraulic$diagnostics; }
    @Inject(method = {"sendUpstreamPacket", "sendUpstreamPacketImmediately"}, at = @At("HEAD"))
    private void hydraulic$countUpstream(BedrockPacket packet, CallbackInfo ci) {
        hydraulic$diagnostics.record((GeyserSession)(Object)this, "bedrock", packet);
    }
    @Inject(method = "disconnect(Lnet/kyori/adventure/text/Component;)V", at = @At("HEAD"))
    private void hydraulic$reportDisconnect(net.kyori.adventure.text.Component reason, CallbackInfo ci) {
        hydraulic$diagnostics.disconnect((GeyserSession)(Object)this);
    }
}
