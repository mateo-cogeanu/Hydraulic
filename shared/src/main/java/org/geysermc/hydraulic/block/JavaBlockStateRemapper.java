package org.geysermc.hydraulic.block;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import org.geysermc.geyser.registry.BlockRegistries;
import org.slf4j.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Converts server state IDs to Geyser's IDs at the protocol/world boundary. */
public final class JavaBlockStateRemapper {
    private static volatile int[] mapping = new int[0];

    private JavaBlockStateRemapper() {
    }

    public static int translate(int serverId) {
        int[] table = mapping;
        return serverId >= 0 && serverId < table.length ? table[serverId] : serverId;
    }

    public static void initialize(Logger logger) {
        Map<String, Integer> vanillaIds = new HashMap<>();
        Map<String, Set<String>> vanillaProperties = new HashMap<>();
        for (var entry : BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get().object2IntEntrySet()) {
            String identifier = entry.getKey();
            if (!identifier.startsWith("minecraft:")) continue;
            var properties = StateIdentifiers.properties(identifier).keySet();
            vanillaProperties.put(StateIdentifiers.block(identifier), Set.copyOf(properties));
            vanillaIds.put(StateIdentifiers.project(identifier, properties), entry.getIntValue());
        }
        int[] table = new int[Block.BLOCK_STATE_REGISTRY.size()];
        int remappedVanilla = 0;
        int fluids = 0;
        int unmatchedVanilla = 0;
        for (int id = 0; id < table.length; id++) {
            table[id] = id;
            var state = Block.BLOCK_STATE_REGISTRY.byId(id);
            if (state == null) continue;
            String identifier = BlockStateParser.serialize(state);
            String block = StateIdentifiers.block(identifier);
            if (block.startsWith("minecraft:")) {
                String projected = StateIdentifiers.project(identifier, vanillaProperties.getOrDefault(block, Set.of()));
                Integer vanillaId = vanillaIds.get(projected);
                if (vanillaId == null) {
                    if (unmatchedVanilla++ < 8) logger.warn("No vanilla state mapping for {}", identifier);
                    table[id] = 0;
                } else {
                    table[id] = vanillaId;
                    if (id != vanillaId) remappedVanilla++;
                }
            } else if (state.getBlock() instanceof LiquidBlock && !BlockRegistries.NON_VANILLA_BLOCK_IDS.get().get(id)) {
                var fluid = (state.getFluidState().is(FluidTags.LAVA) ? Blocks.LAVA : Blocks.WATER).defaultBlockState();
                if (state.hasProperty(LiquidBlock.LEVEL)) fluid = fluid.setValue(LiquidBlock.LEVEL, state.getValue(LiquidBlock.LEVEL));
                table[id] = vanillaIds.getOrDefault(BlockStateParser.serialize(fluid), 0);
                fluids++;
            }
        }
        mapping = table;
        logger.info("Mapped server block states: {} shifted vanilla states, {} fallback fluid states, {} unmatched vanilla states",
                remappedVanilla, fluids, unmatchedVanilla);
    }
}
