package org.geysermc.hydraulic.mixin.ext;

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.VoxelShapesPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.block.VoxelShapeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = GeyserSession.class, remap = false)
public class VoxelShapesMixin {
    @ModifyArg(method = "startGame", at = @At(value = "INVOKE", target = "Lorg/geysermc/geyser/session/UpstreamSession;sendPacket(Lorg/geysermc/geyser/shaded/org/cloudburstmc/protocol/bedrock/packet/BedrockPacket;)V"), index = 0)
    private BedrockPacket hydraulic$supplyBuiltinVoxelShapes(BedrockPacket packet) {
        if (packet instanceof VoxelShapesPacket shapes) VoxelShapeRegistry.supplyBuiltins(shapes);
        return packet;
    }
}
