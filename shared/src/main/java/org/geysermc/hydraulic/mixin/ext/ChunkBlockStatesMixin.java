package org.geysermc.hydraulic.mixin.ext;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.geysermc.geyser.translator.protocol.java.level.JavaLevelChunkWithLightTranslator;
import org.geysermc.hydraulic.block.BlockPaletteRemapper;
import org.geysermc.hydraulic.block.JavaBlockStateRemapper;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = JavaLevelChunkWithLightTranslator.class, remap = false)
public class ChunkBlockStatesMixin {
    @ModifyExpressionValue(
            method = "translate(Lorg/geysermc/geyser/session/GeyserSession;Lorg/geysermc/mcprotocollib/protocol/packet/ingame/clientbound/level/ClientboundLevelChunkWithLightPacket;)V",
            at = @At(value = "INVOKE", target = "Lorg/geysermc/mcprotocollib/protocol/codec/MinecraftTypes;readChunkSection(Lio/netty/buffer/ByteBuf;II)Lorg/geysermc/mcprotocollib/protocol/data/game/chunk/ChunkSection;")
    )
    private ChunkSection hydraulic$remapBlockPalette(ChunkSection original) {
        return new ChunkSection(original.getBlockCount(), original.getFluidCount(),
                BlockPaletteRemapper.remap(original.getBlockData(), JavaBlockStateRemapper::translate), original.getBiomeData());
    }
}
