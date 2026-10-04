package org.geysermc.hydraulic.entity;

import com.google.gson.*;
import java.util.Map;

/** GeckoLib's exported geometry is Bedrock geometry; its keyframe wrapper is not. */
public final class GeckoAssets {
    public static JsonObject geometry(JsonObject source, String identifier) {
        JsonObject result = source.deepCopy();
        for (var geometry : result.getAsJsonArray("minecraft:geometry")) {
            geometry.getAsJsonObject().getAsJsonObject("description").addProperty("identifier", identifier);
        }
        return result;
    }

    public static JsonObject animations(JsonObject source, String prefix) {
        JsonObject animations = new JsonObject();
        for (var entry : source.getAsJsonObject("animations").entrySet()) {
            JsonObject animation = entry.getValue().deepCopy().getAsJsonObject();
            // Gecko callbacks and easing functions have no Bedrock equivalent. Preserve vectors,
            // using Bedrock linear interpolation, rather than exporting invalid keyframes.
            animation.remove("sound_effects");
            animation.remove("particle_effects");
            animation.remove("timeline");
            if (animation.has("bones")) {
                for (var bone : animation.getAsJsonObject("bones").entrySet()) {
                    JsonObject channels = bone.getValue().getAsJsonObject();
                    for (String channel : java.util.List.of("rotation", "position", "scale")) {
                        if (channels.has(channel)) channels.add(channel, unwrap(channels.get(channel)));
                    }
                }
            }
            animations.add(prefix + "." + entry.getKey(), animation);
        }
        JsonObject result = new JsonObject();
        result.addProperty("format_version", "1.8.0");
        result.add("animations", animations);
        return result;
    }

    private static JsonElement unwrap(JsonElement value) {
        if (!value.isJsonObject()) return value;
        JsonObject object = value.getAsJsonObject();
        if (object.has("vector")) return object.get("vector").deepCopy();
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) result.add(entry.getKey(), unwrap(entry.getValue()));
        return result;
    }
}
