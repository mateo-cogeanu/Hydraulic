package org.geysermc.hydraulic.block;

import org.junit.jupiter.api.Test;
import team.unnamed.creative.blockstate.Condition;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class BlockStateConditionsTest {
    @Test
    void unconditionalPostAndEveryConnectedWallArmMatch() {
        var state = Map.of("up", "true", "north", "low", "south", "tall", "east", "none");
        assertTrue(BlockStateConditions.matches(Condition.NONE, state));
        assertTrue(BlockStateConditions.matches(Condition.match("up", "true"), state));
        assertTrue(BlockStateConditions.matches(Condition.match("north", "low|tall"), state));
        assertTrue(BlockStateConditions.matches(Condition.match("south", "low|tall"), state));
        assertFalse(BlockStateConditions.matches(Condition.match("east", "low|tall"), state));
    }

    @Test
    void nestedConditionsAndMissingProperties() {
        var condition = Condition.and(Condition.match("waterlogged", "false"),
                Condition.or(Condition.match("north", "true"), Condition.match("east", "true")));
        assertTrue(BlockStateConditions.matches(condition, Map.of("waterlogged", "false", "east", "true")));
        assertFalse(BlockStateConditions.matches(condition, Map.of("waterlogged", "true", "east", "true")));
        assertFalse(BlockStateConditions.matches(condition, Map.of("east", "true")));
    }

    @Test
    void negatedAlternatives() {
        var condition = Condition.match("axis", "!x|z");
        assertTrue(BlockStateConditions.matches(condition, Map.of("axis", "y")));
        assertFalse(BlockStateConditions.matches(condition, Map.of("axis", "z")));
        assertFalse(BlockStateConditions.matches(condition, Map.of()));
    }
}
