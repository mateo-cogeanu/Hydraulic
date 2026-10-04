package org.geysermc.hydraulic.block;

import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.FallingBlockData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.BreakBlockEventData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.BlockParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.Particle;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;

public final class BlockPacketRemapper {
    private BlockPacketRemapper() {
    }

    public static Packet remap(Packet packet, IntUnaryOperator mapper) {
        if (packet instanceof ClientboundBlockUpdatePacket update) {
            var entry = update.getEntry();
            return update.withEntry(new BlockChangeEntry(entry.getPosition(), mapper.applyAsInt(entry.getBlock())));
        }
        if (packet instanceof ClientboundSectionBlocksUpdatePacket update) {
            var entries = Arrays.stream(update.getEntries())
                    .map(entry -> new BlockChangeEntry(entry.getPosition(), mapper.applyAsInt(entry.getBlock())))
                    .toArray(BlockChangeEntry[]::new);
            return update.withEntries(entries);
        }
        if (packet instanceof ClientboundSetEntityDataPacket metadata) {
            EntityMetadata<?, ?>[] values = metadata.getMetadata().clone();
            for (int index = 0; index < values.length; index++) {
                if (values[index] instanceof IntEntityMetadata value &&
                        (value.getType() == MetadataTypes.BLOCK_STATE || value.getType() == MetadataTypes.OPTIONAL_BLOCK_STATE)) {
                    values[index] = new IntEntityMetadata(value.getId(), value.getType(), mapper.applyAsInt(value.getPrimitiveValue()));
                }
            }
            return metadata.withMetadata(values);
        }
        if (packet instanceof ClientboundAddEntityPacket spawn && spawn.getData() instanceof FallingBlockData block) {
            return spawn.withData(new FallingBlockData(mapper.applyAsInt(block.getId()), block.getMetadata()));
        }
        if (packet instanceof ClientboundLevelEventPacket event && event.getData() instanceof BreakBlockEventData block) {
            return event.withData(new BreakBlockEventData(mapper.applyAsInt(block.getBlockState())));
        }
        if (packet instanceof ClientboundLevelParticlesPacket particles && particles.getParticle().getData() instanceof BlockParticleData block) {
            return particles.withParticle(new Particle(particles.getParticle().getType(), new BlockParticleData(mapper.applyAsInt(block.getBlockState()))));
        }
        return packet;
    }
}
