package org.geysermc.hydraulic.mixin.ext;

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.java.JavaRespawnTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JavaRespawnTranslator.class, remap = false)
public class DimensionViewDistanceMixin {
    @Inject(method = "translate(Lorg/geysermc/geyser/session/GeyserSession;Lorg/geysermc/mcprotocollib/protocol/packet/ingame/clientbound/ClientboundRespawnPacket;)V", at = @At("TAIL"))
    private void hydraulic$refreshWorldView(GeyserSession session, ClientboundRespawnPacket packet, CallbackInfo ci) {
        session.sendJavaClientSettings();
    }
}
