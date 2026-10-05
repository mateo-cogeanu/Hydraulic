package org.geysermc.hydraulic.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.level.chunk.GeyserChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;

/** Keep the native water layer at Ichor's real flow level, below its textured surface. */
public final class IchorFluidLayer {
    public static final Map<Integer,Integer> LEVELS = new ConcurrentHashMap<>();
    public static org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition fluid(BlockMappings mappings, int javaId) {
        Integer level=LEVELS.get(javaId); if(level==null) return mappings.getBedrockWater();
        int water=BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get().getInt("minecraft:water[level="+level+"]");
        return mappings.getBedrockBlock(water);
    }
    public static void apply(BlockMappings mappings, DataPalette[] javaChunks, GeyserChunkSection[] sections, int offset) {
        for(int section=0;section<javaChunks.length;section++) {
            var palette=javaChunks[section]; int bedrock=section+offset;
            if(palette==null || bedrock<0 || bedrock>=sections.length || sections[bedrock]==null || sections[bedrock].getBlockStorageArray().length<2)continue;
            boolean ichor=palette.getPalette() instanceof org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.GlobalPalette;
            for(int i=0;!ichor && i<palette.getPalette().size();i++) if(LEVELS.containsKey(palette.getPalette().idToState(i))) {ichor=true;break;}
            if(!ichor && !(palette.getPalette() instanceof org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.GlobalPalette))continue;
            var layers = sections[bedrock].getBlockStorageArray();
            boolean copied = false;
            for(int x=0;x<16;x++) for(int y=0;y<16;y++) for(int z=0;z<16;z++) {
                int id=palette.get(x,y,z);if(!LEVELS.containsKey(id))continue;
                int index=(x<<8)|(z<<4)|y, runtime=fluid(mappings,id).getRuntimeId();
                if(layers[1].getFullBlock(index)==runtime)continue;
                // Geyser constructs water layers with immutable singleton/two-entry palettes.
                // Copy lazily before adding a depth, retaining every non-Ichor cell.
                if(!copied) { layers[1]=layers[1].copy(); copied=true; }
                layers[1].setFullBlock(index,runtime);
            }
        }
    }
}
