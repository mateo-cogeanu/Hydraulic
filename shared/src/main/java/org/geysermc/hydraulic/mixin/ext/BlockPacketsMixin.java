package org.geysermc.hydraulic.mixin.ext;

import org.geysermc.geyser.registry.PacketTranslatorRegistry;
import org.geysermc.hydraulic.block.BlockPacketRemapper;
import org.geysermc.hydraulic.block.JavaBlockStateRemapper;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = PacketTranslatorRegistry.class, remap = false)
public class BlockPacketsMixin {
    @ModifyVariable(method = "translate0", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Object hydraulic$remapBlockStatePacket(Object packet) {
        return packet instanceof Packet javaPacket
                ? BlockPacketRemapper.remap(javaPacket, JavaBlockStateRemapper::translate) : packet;
    }
}
