package org.geysermc.hydraulic.block;

import com.google.gson.*;
import java.util.*;

/** Cull only unrotated boundary faces against fully occluding neighboring blocks. */
public final class GeometryCulling {
    public static String identifier(String geometry) {
        return "hydraulic:cull_" + geometry.replace('.', '_').replace(':', '_');
    }
    public static JsonObject definition(JsonObject geometry) {
        Map<String, JsonObject> bones = new HashMap<>();
        for (var bone : geometry.getAsJsonArray("bones")) bones.put(bone.getAsJsonObject().get("name").getAsString(), bone.getAsJsonObject());
        JsonArray rules = new JsonArray();
        for (var bone : bones.values()) {
            if (rotated(bone, bones, new HashSet<>()) || !bone.has("cubes")) continue;
            int index = 0;
            for (var value : bone.getAsJsonArray("cubes")) {
                JsonObject cube = value.getAsJsonObject(); int cubeIndex = index++;
                if (hasRotation(cube) || cube.has("inflate") || !cube.has("uv") || !cube.get("uv").isJsonObject()) continue;
                var origin = cube.getAsJsonArray("origin"); var size = cube.getAsJsonArray("size");
                if (origin == null || size == null) continue;
                for (String face : cube.getAsJsonObject("uv").keySet()) {
                    int axis = switch (face) { case "east", "west" -> 0; case "up", "down" -> 1; default -> 2; };
                    boolean positive = Set.of("east", "up", "south").contains(face);
                    double plane = origin.get(axis).getAsDouble() + (positive ? size.get(axis).getAsDouble() : 0);
                    double boundary = axis == 1 ? (positive ? 16 : 0) : (positive ? 8 : -8);
                    if (Math.abs(plane-boundary) > 0.00001) continue;
                    // The adjacent block can cover only the face's portion inside its unit volume.
                    boolean contained = true;
                    for (int other = 0; other < 3; other++) {
                        if (other == axis) continue;
                        double start = origin.get(other).getAsDouble();
                        double end = start + size.get(other).getAsDouble();
                        double minimum = other == 1 ? 0 : -8;
                        double maximum = other == 1 ? 16 : 8;
                        if (start < minimum - 0.00001 || end > maximum + 0.00001 || end < start) contained = false;
                    }
                    if (!contained) continue;
                    JsonObject part = new JsonObject(); part.addProperty("bone", bone.get("name").getAsString()); part.addProperty("cube", cubeIndex); part.addProperty("face", face);
                    JsonObject rule = new JsonObject(); rule.add("geometry_part", part); rule.addProperty("direction", face); rules.add(rule);
                }
            }
        }
        String id = geometry.getAsJsonObject("description").get("identifier").getAsString();
        JsonObject description = new JsonObject(); description.addProperty("identifier", identifier(id));
        JsonObject body = new JsonObject(); body.add("description", description); body.add("rules", rules);
        JsonObject document = new JsonObject(); document.addProperty("format_version", "1.20.60"); document.add("minecraft:block_culling_rules", body); return document;
    }
    private static boolean hasRotation(JsonObject object) {
        if (!object.has("rotation")) return false;
        for (var angle : object.getAsJsonArray("rotation")) if (Math.abs(angle.getAsDouble()) > 0.00001) return true;
        return false;
    }
    private static boolean rotated(JsonObject bone, Map<String, JsonObject> bones, Set<String> visited) {
        if (hasRotation(bone) || bone.has("inflate")) return true;
        if (!bone.has("parent")) return false;
        String parent = bone.get("parent").getAsString();
        return !visited.add(parent) || !bones.containsKey(parent) || rotated(bones.get(parent), bones, visited);
    }
}
