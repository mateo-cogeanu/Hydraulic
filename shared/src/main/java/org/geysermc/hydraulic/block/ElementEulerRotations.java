package org.geysermc.hydraulic.block;

import com.google.gson.*;
import net.kyori.adventure.key.Key;
import org.geysermc.pack.converter.type.model.ModelStitcher;
import team.unnamed.creative.model.Model;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Preserve 26.3 multi-axis element rotations lost by the older model API. */
public final class ElementEulerRotations {
    private static final Map<Key, Map<Integer, JsonArray>> ROTATIONS = new ConcurrentHashMap<>();
    public static void capture(JsonElement source, Key key) {
        if (!source.isJsonObject() || !source.getAsJsonObject().has("elements") || source.getAsJsonObject().getAsJsonArray("elements").isEmpty()) {
            ROTATIONS.remove(key);
            return;
        }
        var rotations = new HashMap<Integer, JsonArray>();
        int index = 0;
        for (var value : source.getAsJsonObject().getAsJsonArray("elements")) {
            var element = value.getAsJsonObject();
            if (element.has("rotation")) {
                var rotation = element.getAsJsonObject("rotation");
                if (rotation.has("x") || rotation.has("y") || rotation.has("z")) {
                    var angles = new JsonArray();
                    for (String axis : List.of("x", "y", "z")) angles.add(rotation.has(axis) ? -rotation.get(axis).getAsFloat() : 0f);
                    rotations.put(index, angles);
                }
            }
            index++;
        }
        ROTATIONS.put(key, rotations);
    }
    public static void apply(JsonObject document, Model model, ModelStitcher.Provider provider) {
        var visited = new HashSet<Key>();
        while (model != null && visited.add(model.key())) {
            var rotations = ROTATIONS.get(model.key());
            if (rotations != null) {
                var bones = document.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
                for (var value : bones) {
                    var bone = value.getAsJsonObject();
                    String name = bone.get("name").getAsString();
                    if (!name.startsWith("bone_") || !bone.has("cubes")) continue;
                    var angles = rotations.get(Integer.parseInt(name.substring(5)));
                    if (angles != null) for (var cube : bone.getAsJsonArray("cubes")) cube.getAsJsonObject().add("rotation", angles.deepCopy());
                }
                return;
            }
            model = model.parent() == null ? null : provider.model(model.parent());
        }
    }
}
