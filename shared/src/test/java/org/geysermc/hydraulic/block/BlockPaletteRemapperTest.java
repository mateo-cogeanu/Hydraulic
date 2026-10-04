package org.geysermc.hydraulic.block;

import org.junit.jupiter.api.Test;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.*;
import static org.junit.jupiter.api.Assertions.*;

class BlockPaletteRemapperTest {
    @Test
    void singletonIsTranslated() {
        var original = DataPalette.create(new SingletonPalette(45000), null, PaletteType.BLOCK_STATE, 65536);
        var mapped = BlockPaletteRemapper.remap(original, id -> 12);
        assertInstanceOf(SingletonPalette.class, mapped.getPalette());
        assertEquals(12, mapped.get(0, 0, 0));
    }

    @Test
    void listPaletteKeepsIndicesEvenWhenStatesMerge() {
        var original = DataPalette.createForBlockState(0, 65536);
        original.set(0, 0, 0, 44000);
        original.set(1, 0, 0, 44001);
        original.set(2, 0, 0, 53000);
        var mapped = BlockPaletteRemapper.remap(original, id -> id == 44000 || id == 44001 ? 123 : id);
        assertEquals(123, mapped.get(0, 0, 0));
        assertEquals(123, mapped.get(1, 0, 0));
        assertEquals(53000, mapped.get(2, 0, 0));
        mapped.set(3, 0, 0, 123);
        assertEquals(123, mapped.get(3, 0, 0));
        assertEquals(53000, mapped.get(2, 0, 0));
    }

    @Test
    void mapPaletteCanStillAcceptSubsequentUpdates() {
        var original = DataPalette.createForBlockState(0, 65536);
        for (int i = 0; i < 40; i++) original.set(i & 15, i >> 4, 0, 44000 + i);
        assertInstanceOf(MapPalette.class, original.getPalette());
        var mapped = BlockPaletteRemapper.remap(original, id -> id >= 44000 ? id - 43000 : id);
        for (int i = 0; i < 40; i++) assertEquals(1000 + i, mapped.get(i & 15, i >> 4, 0));
        mapped.set(0, 0, 0, 1039);
        assertEquals(1039, mapped.get(0, 0, 0));
        assertEquals(1001, mapped.get(1, 0, 0));
    }

    @Test
    void globalPaletteTranslatesRuntimeIdsInStorage() {
        var original = DataPalette.createForBlockState(0, 65536);
        for (int i = 0; i < 300; i++) original.set(i & 15, (i >> 8) & 15, (i >> 4) & 15, 44000 + i);
        assertInstanceOf(GlobalPalette.class, original.getPalette());
        var mapped = BlockPaletteRemapper.remap(original, id -> id >= 44000 ? id - 43000 : id);
        for (int i = 0; i < 300; i++) assertEquals(1000 + i, mapped.get(i & 15, (i >> 8) & 15, (i >> 4) & 15));
    }

    @Test
    void biomePaletteIsNeverRemapped() {
        var biome = DataPalette.createForBiome(0, 256);
        biome.set(0, 0, 0, 50);
        assertSame(biome, BlockPaletteRemapper.remap(biome, id -> {throw new AssertionError("Biome is not a block");}));
    }
}
