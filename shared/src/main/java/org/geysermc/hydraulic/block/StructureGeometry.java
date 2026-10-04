package org.geysermc.hydraulic.block;

import com.google.gson.*;
import java.util.Map;
import java.util.List;

/** Explicit opaque frame geometry, without relying on a client built-in full-block alias. */
public final class StructureGeometry {
    public static final String IDENTIFIER = "geometry.hydraulic.structure_cube";
    public static boolean isSiftFrame(String identifier) {
        return java.util.Set.of("the_sift:siftslate", "the_sift:siftslate_growth", "the_sift:healthy_sculk", "the_sift:reinforced_siftslate").contains(identifier);
    }
    public static JsonObject create() {
        JsonObject uv = new JsonObject();
        Gson gson = new Gson();
        for (String face : List.of("north", "south", "east", "west", "up", "down")) {
            uv.add(face, gson.toJsonTree(Map.of("uv", List.of(0, 0), "uv_size", List.of(16, 16), "material_instance", face)));
        }
        return gson.toJsonTree(Map.of("format_version", "1.16.0", "minecraft:geometry", List.of(Map.of(
                "description", Map.of("identifier", IDENTIFIER, "texture_width", 16, "texture_height", 16),
                "bones", List.of(Map.of("name", "frame", "pivot", List.of(0, 8, 0), "cubes", List.of(Map.of(
                        "origin", List.of(-8, 0, -8), "size", List.of(16, 16, 16), "uv", uv)))))))).getAsJsonObject();
    }
}
