package org.geysermc.hydraulic.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MissingMappingsTest {
    @Test
    void gapsBeforeFirstCustomBlockAreCompleted() {
        String[] table = {"air", "stone", null, null, "siftslate"};
        assertEquals(2, MissingMappings.fill(table, id -> "fluid_" + id));
        assertArrayEquals(new String[]{"air", "stone", "fluid_2", "fluid_3", "siftslate"}, table);
        for (String mapping : table) assertNotNull(mapping);
    }

    @Test
    void existingMappingsAndRepeatedRepairsArePreserved() {
        String[] table = {"air", "custom_geometry", "water"};
        assertEquals(0, MissingMappings.fill(table, id -> { throw new AssertionError("Already mapped"); }));
        assertArrayEquals(new String[]{"air", "custom_geometry", "water"}, table);
    }

    @Test
    void nullFallbackCannotLeaveTheTableSilentlyIncomplete() {
        assertThrows(NullPointerException.class, () -> MissingMappings.fill(new String[1], id -> null));
        assertEquals(0, MissingMappings.fill(new String[0], id -> null));
    }
}
