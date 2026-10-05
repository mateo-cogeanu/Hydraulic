package org.geysermc.hydraulic.mixin.ext;

import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.java.level.JavaSectionBlocksUpdateTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import org.geysermc.hydraulic.block.IchorFluidLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=JavaSectionBlocksUpdateTranslator.class,remap=false)
public class IchorBulkUpdatesMixin {
    @Inject(method="translate(Lorg/geysermc/geyser/session/GeyserSession;Lorg/geysermc/mcprotocollib/protocol/packet/ingame/clientbound/level/ClientboundSectionBlocksUpdatePacket;)V",at=@At("RETURN"))
    private void hydraulic$flowLevels(GeyserSession session, ClientboundSectionBlocksUpdatePacket packet, CallbackInfo callback) {
        for(var entry:packet.getEntries()) {
            if(!IchorFluidLayer.LEVELS.containsKey(entry.getBlock()))continue;
            UpdateBlockPacket water=new UpdateBlockPacket();water.setBlockPosition(entry.getPosition());water.setDataLayer(1);
            water.setDefinition(IchorFluidLayer.fluid(session.getBlockMappings(),entry.getBlock()));water.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
            session.sendUpstreamPacket(water);
        }
    }
}
