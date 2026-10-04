package org.geysermc.hydraulic.block;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class StateIdentifiersTest {
    @Test
    void addedIchorPropertyDoesNotChangeVanillaWallIdentity() {
        assertEquals("minecraft:stone_brick_wall[east=low,north=none,south=tall,up=true,waterlogged=false,west=tall]",
                StateIdentifiers.project("minecraft:stone_brick_wall[east=low,ichorlogged=true,north=none,south=tall,up=true,waterlogged=false,west=tall]",
                        Set.of("east", "north", "south", "up", "waterlogged", "west")));
    }

    @Test
    void legitimateVanillaPropertiesArePreservedAndSorted() {
        assertEquals("minecraft:oak_stairs[facing=east,half=top,shape=inner_left,waterlogged=true]",
                StateIdentifiers.project("minecraft:oak_stairs[waterlogged=true,shape=inner_left,half=top,ichorlogged=false,facing=east]",
                        Set.of("facing", "half", "shape", "waterlogged")));
    }

    @Test
    void UnextendedSingleStateAndPropertylessProjection() {
        assertEquals("minecraft:stone", StateIdentifiers.project("minecraft:stone", Set.of()));
        assertEquals("minecraft:stone", StateIdentifiers.project("minecraft:stone[custom=true]", Set.of()));
    }
}
