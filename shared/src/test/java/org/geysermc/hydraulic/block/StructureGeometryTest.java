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
    @Test void portalIsFullCubeWithMatchingFaceCulling() {
        var geometry = StructureGeometry.create("geometry.hydraulic.sift_portal", "frame")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        var bone = geometry.getAsJsonArray("bones").get(0).getAsJsonObject();
        assertEquals("frame", bone.get("name").getAsString());
        var cube = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();
        assertEquals("[-8,0,-8]", cube.get("origin").toString());
        assertEquals("[16,16,16]", cube.get("size").toString());
        var rules = StructureGeometry.culling().getAsJsonObject("minecraft:block_culling_rules").getAsJsonArray("rules");
        assertEquals(6, rules.size());
        for (var rule : rules) {
            var obj = rule.getAsJsonObject();
            var part = obj.getAsJsonObject("geometry_part");
            assertEquals("frame", part.get("bone").getAsString());
            assertEquals(obj.get("direction"), part.get("face"));
        }
    }
}
