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
    public static boolean isSiftSolidCube(String identifier) {
        return isSiftFrame(identifier) || identifier.equals("the_sift:dry_healthy_sculk");
    }
    public static JsonObject create() {
        return create(IDENTIFIER, "frame");
    }
    public static JsonObject create(String identifier, String bone) {
        JsonObject uv = new JsonObject();
        Gson gson = new Gson();
        for (String face : List.of("north", "south", "east", "west", "up", "down")) {
            uv.add(face, gson.toJsonTree(Map.of("uv", List.of(0, 0), "uv_size", List.of(16, 16), "material_instance", face)));
        }
        return gson.toJsonTree(Map.of("format_version", "1.16.0", "minecraft:geometry", List.of(Map.of(
                "description", Map.of("identifier", identifier, "texture_width", 16, "texture_height", 16),
                "bones", List.of(Map.of("name", bone, "pivot", List.of(0, 8, 0), "cubes", List.of(Map.of(
                        "origin", List.of(-8, 0, -8), "size", List.of(16, 16, 16), "uv", uv)))))))).getAsJsonObject();
    }
    public static final String CULLING = "hydraulic:solid_cube";
    public static JsonObject culling() {
        var rules = new com.google.gson.JsonArray();
        Gson gson = new Gson();
        for (String face : List.of("north", "south", "east", "west", "up", "down")) {
            rules.add(gson.toJsonTree(Map.of("geometry_part", Map.of("bone", "frame", "cube", 0, "face", face), "direction", face)));
        }
        return gson.toJsonTree(Map.of("format_version", "1.20.60", "minecraft:block_culling_rules", Map.of(
                "description", Map.of("identifier", CULLING), "rules", rules))).getAsJsonObject();
    }
}
