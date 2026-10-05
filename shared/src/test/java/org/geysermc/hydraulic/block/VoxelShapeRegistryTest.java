package org.geysermc.hydraulic.block;

import org.cloudburstmc.protocol.bedrock.packet.VoxelShapesPacket;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VoxelShapeRegistryTest {
    @Test void emptyLoginRegistryGainsValidDistinctBuiltinHandlesAndCellOccupancy() {
        var packet = new VoxelShapesPacket(); packet.setShapes(List.of()); packet.setNameMap(Map.of());
        VoxelShapeRegistry.supplyBuiltins(packet);
        assertEquals(2, packet.getShapes().size());
        for (var entry : packet.getNameMap().entrySet()) {
            var shape = packet.getShapes().get(entry.getValue());
            assertEquals(List.of(0f, 1f), shape.getXCoordinates());
            assertEquals(1, shape.getCells().getXSize());
            assertEquals(List.of((short) (entry.getKey().endsWith("empty") ? 0 : 1)), shape.getCells().getStorage());
        }
        assertEquals(0, packet.getCustomShapeCount());
        VoxelShapeRegistry.supplyBuiltins(packet);
        assertEquals(2, packet.getShapes().size());
    }
    @Test void preservesExistingShapesAndRepairsInvalidBuiltinHandles() {
        var packet = new VoxelShapesPacket(); packet.setShapes(List.of());
        packet.setNameMap(Map.of("minecraft:empty", 999));
        VoxelShapeRegistry.supplyBuiltins(packet);
        var first = packet.getShapes().get(packet.getNameMap().get("minecraft:unit_cube"));
        packet.getNameMap().put("custom:original", 1); packet.setCustomShapeCount(1);
        VoxelShapeRegistry.supplyBuiltins(packet);
        assertSame(first, packet.getShapes().get(1));
        assertEquals(1, packet.getNameMap().get("custom:original"));
        assertEquals(1, packet.getCustomShapeCount());
    }
}
