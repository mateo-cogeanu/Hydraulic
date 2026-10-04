package org.geysermc.hydraulic.mixin.ext;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.GeyserSessionAdapter;
import org.geysermc.hydraulic.compat.SessionTrafficAccess;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = GeyserSessionAdapter.class, remap = false)
public class PacketErrorDiagnosticsMixin {
    @Shadow @Final private GeyserSession session;
    @Inject(method = "packetError", at = @At("HEAD"))
    private void hydraulic$reportDecodeError(PacketErrorEvent event, CallbackInfo ci) {
        ((SessionTrafficAccess)session).hydraulic$traffic().error(session, event);
    }
}
