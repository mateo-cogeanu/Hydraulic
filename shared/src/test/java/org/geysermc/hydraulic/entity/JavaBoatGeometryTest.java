package org.geysermc.hydraulic.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JavaBoatGeometryTest {
    @Test void exportsHullPaddlesAndChestWithNativeTextureDimensions() {
        for(boolean chest:new boolean[]{false,true}) {
            var geometry=JavaBoatGeometry.create("geometry.test.boat",chest).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            assertEquals(chest?10:7,geometry.getAsJsonArray("bones").size());
            assertEquals(chest?128:64,geometry.getAsJsonObject("description").get("texture_height").getAsInt());
            int cubes=0;
            for(var b:geometry.getAsJsonArray("bones")) {
                for(var c:b.getAsJsonObject().getAsJsonArray("cubes")) {
                    cubes++;
                    for(var size:c.getAsJsonObject().getAsJsonArray("size")) assertTrue(size.getAsFloat()>0);
                }
            }
            assertEquals(chest?12:9,cubes);
        }
    }
}
