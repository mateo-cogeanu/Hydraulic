package org.geysermc.hydraulic.block;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.*;
import java.util.function.IntUnaryOperator;

/** Converts only block palettes; biome palettes and palette indices remain unchanged. */
public final class BlockPaletteRemapper {
    private BlockPaletteRemapper() {
    }

    public static DataPalette remap(DataPalette data, IntUnaryOperator mapper) {
        if (data.getPaletteType() != PaletteType.BLOCK_STATE) return data;
        Palette palette = data.getPalette();
        if (palette instanceof GlobalPalette) {
            for (int index = 0; index < 4096; index++) {
                int original = data.getStorage().get(index);
                data.getStorage().set(index, mapper.applyAsInt(original));
            }
            return data;
        }
        if (palette instanceof SingletonPalette) {
            int original = palette.idToState(0);
            int mapped = mapper.applyAsInt(original);
            if (original == mapped) return data;
            return DataPalette.create(new SingletonPalette(mapped), null, data.getPaletteType(), (1 << data.getGlobalPaletteBitsPerEntry()));
        }
        int[] values = new int[palette.size()];
        boolean changed = false;
        for (int index = 0; index < values.length; index++) {
            int original = palette.idToState(index);
            values[index] = mapper.applyAsInt(original);
            changed |= values[index] != original;
        }
        if (!changed) return data;
        ByteBuf buffer = Unpooled.buffer();
        try {
            writeVarInt(buffer, values.length);
            for (int value : values) writeVarInt(buffer, value);
            int bits = data.getStorage().getBitsPerEntry();
            Palette converted = palette instanceof ListPalette ? new ListPalette(bits, buffer) : new MapPalette(bits, buffer);
            return DataPalette.create(converted, data.getStorage(), data.getPaletteType(), (1 << data.getGlobalPaletteBitsPerEntry()));
        } finally {
            buffer.release();
        }
    }

    private static void writeVarInt(ByteBuf buffer, int value) {
        while ((value & ~0x7F) != 0) {
            buffer.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buffer.writeByte(value);
    }
}
