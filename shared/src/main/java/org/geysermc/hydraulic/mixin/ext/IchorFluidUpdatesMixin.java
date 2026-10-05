package org.geysermc.hydraulic.mixin.ext;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.block.IchorFluidLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Block.class, remap = false)
public class IchorFluidUpdatesMixin {
    @Inject(method="updateBlock",at=@At("RETURN"))
    private void hydraulic$flowLevel(GeyserSession session, BlockState state, Vector3i position, CallbackInfo callback) {
        if(!IchorFluidLayer.LEVELS.containsKey(state.javaId()))return;
        UpdateBlockPacket water=new UpdateBlockPacket();water.setDataLayer(1);water.setBlockPosition(position);
        water.setDefinition(IchorFluidLayer.fluid(session.getBlockMappings(),state.javaId()));water.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(water);
    }
}
