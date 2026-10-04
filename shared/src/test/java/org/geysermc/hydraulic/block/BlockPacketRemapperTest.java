package org.geysermc.hydraulic.block;

import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import org.junit.jupiter.api.Test;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import static org.junit.jupiter.api.Assertions.*;

class BlockPacketRemapperTest {
    @Test
    void blockMetadataIsMappedWithoutChangingEmptyOptionalStatesOrOtherIntegers() {
        var untouched = new IntEntityMetadata(3, MetadataTypes.INT, 45000);
        var original = new ClientboundSetEntityDataPacket(7, new EntityMetadata<?, ?>[]{
                new IntEntityMetadata(1, MetadataTypes.BLOCK_STATE, 45000),
                new IntEntityMetadata(2, MetadataTypes.OPTIONAL_BLOCK_STATE, 0), untouched});
        var mapped = (ClientboundSetEntityDataPacket) BlockPacketRemapper.remap(original, id -> id == 45000 ? 123 : id);
        assertEquals(123, mapped.getMetadata()[0].getValue());
        assertEquals(0, mapped.getMetadata()[1].getValue());
        assertSame(untouched, mapped.getMetadata()[2]);
        assertEquals(45000, original.getMetadata()[0].getValue());
    }

    @Test
    void updateTranslatesStateButPreservesPositionAndOriginalPacket() {
        var position = Vector3i.from(4, 70, 5);
        var original = new ClientboundBlockUpdatePacket(new BlockChangeEntry(position, 45000));
        var mapped = (ClientboundBlockUpdatePacket) BlockPacketRemapper.remap(original, id -> 123);
        assertEquals(position, mapped.getEntry().getPosition());
        assertEquals(123, mapped.getEntry().getBlock());
        assertEquals(45000, original.getEntry().getBlock());
    }

    @Test
    void sectionUpdatesTranslateEveryEntryIncludingCustomStates() {
        var original = new ClientboundSectionBlocksUpdatePacket(0, 4, 0,
                new BlockChangeEntry(Vector3i.from(1, 65, 1), 45000),
                new BlockChangeEntry(Vector3i.from(2, 65, 1), 53000));
        var mapped = (ClientboundSectionBlocksUpdatePacket) BlockPacketRemapper.remap(original, id -> id == 45000 ? 123 : id);
        assertEquals(123, mapped.getEntries()[0].getBlock());
        assertEquals(53000, mapped.getEntries()[1].getBlock());
        assertEquals(original.getEntries()[1].getPosition(), mapped.getEntries()[1].getPosition());
    }
}
