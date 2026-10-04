package org.geysermc.hydraulic.block;

import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.hydraulic.util.MissingMappings;
import org.slf4j.Logger;

/** Protects remaining unsupported IDs without replacing any registered block definition. */
public final class BlockMappingFallbacks {
    private BlockMappingFallbacks() {
    }

    public static void repair(Logger logger) {
        JavaBlockStateRemapper.initialize(logger);
        int count = 0;
        for (BlockMappings mappings : BlockRegistries.BLOCKS.get().values()) {
            count += MissingMappings.fill(mappings.getJavaToBedrockBlocks(), id -> mappings.getBedrockAir());
            MissingMappings.fill(mappings.getJavaToVanillaBedrockBlocks(), id -> mappings.getBedrockAir());
            // Check the actual chunk-translation lookup for every server state, on every supported palette.
            for (int id = 0; id < net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.size(); id++) {
                mappings.getBedrockBlockId(JavaBlockStateRemapper.translate(id));
            }
        }
        if (count > 0) logger.info("Completed {} unused/unsupported mapping slots across Bedrock palettes", count);
    }
}
