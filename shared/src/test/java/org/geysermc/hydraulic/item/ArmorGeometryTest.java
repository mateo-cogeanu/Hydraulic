package org.geysermc.hydraulic.item;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ArmorGeometryTest {
    @Test void rendersOnlyTheBonesCoveredByEachArmorSlot() {
        Map<String, Set<String>> expected = Map.of("head", Set.of("head"), "chest", Set.of("body", "leftArm", "rightArm"), "legs", Set.of("body", "leftLeg", "rightLeg"), "feet", Set.of("leftLeg", "rightLeg"));
        expected.forEach((slot, visible) -> {
            var geometry = ArmorGeometry.create(slot).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            Set<String> actual = new HashSet<>();
            for (var value : geometry.getAsJsonArray("bones")) {
                var bone = value.getAsJsonObject();
                if (bone.has("cubes")) actual.add(bone.get("name").getAsString());
            }
            assertEquals(visible, actual, slot);
            assertEquals(64, geometry.getAsJsonObject("description").get("texture_width").getAsInt());
            assertEquals(32, geometry.getAsJsonObject("description").get("texture_height").getAsInt());
        });
    }
}
