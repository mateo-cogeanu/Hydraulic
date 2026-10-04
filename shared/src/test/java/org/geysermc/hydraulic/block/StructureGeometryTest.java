package org.geysermc.hydraulic.block;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class StructureGeometryTest {
    @Test void frameHasSixTexturableFacesAndFullBlockBounds() {
        var geometry = StructureGeometry.create().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        var cube = geometry.getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("cubes").get(0).getAsJsonObject();
        assertEquals(Set.of("north", "south", "east", "west", "up", "down"), cube.getAsJsonObject("uv").keySet());
        for (var coordinate : cube.getAsJsonArray("size")) assertEquals(16, coordinate.getAsInt());
        assertTrue(StructureGeometry.isSiftFrame("the_sift:reinforced_siftslate"));
        assertFalse(StructureGeometry.isSiftFrame("the_sift:sift_portal"));
    }
}
