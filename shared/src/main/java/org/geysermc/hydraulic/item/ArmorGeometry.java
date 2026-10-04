package org.geysermc.hydraulic.item;

import com.google.gson.*;
import java.util.*;

/** Explicit Java humanoid armor UV layout, independent of built-in Bedrock aliases. */
public final class ArmorGeometry {
    private static final Gson GSON = new Gson();
    public static JsonObject create(String slot) {
        List<Map<String, Object>> bones = new ArrayList<>();
        bones.add(new LinkedHashMap<>(Map.of("name", "waist", "pivot", List.of(0, 12, 0))));
        bone(bones, "body", "waist", List.of(0, 24, 0), List.of(-4, 12, -2), List.of(8, 12, 4), List.of(16, 16), slot.equals("chest") || slot.equals("legs"), false, slot);
        bone(bones, "head", "body", List.of(0, 24, 0), List.of(-4, 24, -4), List.of(8, 8, 8), List.of(0, 0), slot.equals("head"), false, slot);
        bone(bones, "rightArm", "body", List.of(-5, 22, 0), List.of(-8, 12, -2), List.of(4, 12, 4), List.of(40, 16), slot.equals("chest"), false, slot);
        bone(bones, "leftArm", "body", List.of(5, 22, 0), List.of(4, 12, -2), List.of(4, 12, 4), List.of(40, 16), slot.equals("chest"), true, slot);
        bone(bones, "rightLeg", "body", List.of(-2, 12, 0), List.of(-4, 0, -2), List.of(4, 12, 4), List.of(0, 16), slot.equals("legs") || slot.equals("feet"), false, slot);
        bone(bones, "leftLeg", "body", List.of(2, 12, 0), List.of(0, 0, -2), List.of(4, 12, 4), List.of(0, 16), slot.equals("legs") || slot.equals("feet"), true, slot);
        return GSON.toJsonTree(Map.of("format_version", "1.12.0", "minecraft:geometry", List.of(Map.of("description", Map.of(
                "identifier", "geometry.hydraulic.armor." + slot, "texture_width", 64, "texture_height", 32,
                "visible_bounds_width", 3, "visible_bounds_height", 3, "visible_bounds_offset", List.of(0, 1.5, 0)), "bones", bones)))).getAsJsonObject();
    }
    private static void bone(List<Map<String, Object>> bones, String name, String parent, List<Integer> pivot, List<Integer> origin,
                             List<Integer> size, List<Integer> uv, boolean visible, boolean mirror, String slot) {
        Map<String, Object> bone = new LinkedHashMap<>(Map.of("name", name, "parent", parent, "pivot", pivot));
        if (visible) bone.put("cubes", List.of(Map.of("origin", origin, "size", size, "uv", uv, "mirror", mirror, "inflate", slot.equals("legs") ? 0.5 : 1.0)));
        bones.add(bone);
    }
}
