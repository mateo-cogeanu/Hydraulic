package org.geysermc.hydraulic.block;

import org.cloudburstmc.protocol.bedrock.data.SerializableVoxelShape;
import org.cloudburstmc.protocol.bedrock.packet.VoxelShapesPacket;
import java.util.*;

/** Supply the built-in shapes omitted by Geyser's empty login registry. */
public final class VoxelShapeRegistry {
    private VoxelShapeRegistry() {}

    public static void supplyBuiltins(VoxelShapesPacket packet) {
        var shapes = new ArrayList<>(packet.getShapes());
        var names = new HashMap<>(packet.getNameMap());
        addIfMissing(shapes, names, "minecraft:empty", (short) 0);
        addIfMissing(shapes, names, "minecraft:unit_cube", (short) 1);
        packet.setShapes(shapes);
        packet.setNameMap(names);
        // These are built-in shapes; retain the original custom shape count.
    }

    private static void addIfMissing(List<SerializableVoxelShape> shapes, Map<String, Integer> names, String name, short solid) {
        Integer handle = names.get(name);
        if (handle != null && handle >= 0 && handle < shapes.size()) return;
        var cells = new SerializableVoxelShape.SerializableCells((short) 1, (short) 1, (short) 1, List.of(solid));
        var coordinates = List.of(0f, 1f);
        names.put(name, shapes.size());
        shapes.add(new SerializableVoxelShape(cells, coordinates, coordinates, coordinates));
    }
}
