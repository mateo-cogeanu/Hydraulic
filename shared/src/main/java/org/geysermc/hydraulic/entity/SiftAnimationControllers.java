package org.geysermc.hydraulic.entity;

import com.google.gson.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phase controllers driven by native Sift ticks forwarded in variant/mark_variant. */
public final class SiftAnimationControllers {
    private static final Gson GSON = new Gson();
    public static boolean supports(String name) {
        return List.of("singer", "rift", "mini_rift").contains(name);
    }
    public static Map<Integer, String> phases(String name) {
        return name.equals("singer") ? Map.of(0, "idle", 1, "walk", 2, "appear", 3, "sing", 4, "disappear", 5, "receive_soul", 6, "barter_hold")
                : Map.of(0, "open", 1, "appear", 2, "close");
    }
    public static JsonObject controller(String name) {
        Map<String, Object> states = new LinkedHashMap<>();
        var phases = phases(name);
        for (var phase : phases.entrySet()) {
            var transitions = phases.entrySet().stream().filter(other -> !other.getKey().equals(phase.getKey()))
                    .map(other -> Map.of(other.getValue(), "query.variant == " + other.getKey())).toList();
            states.put(phase.getValue(), Map.of("animations", List.of(phase.getValue()), "transitions", transitions, "blend_transition", 0));
        }
        return GSON.toJsonTree(Map.of("format_version", "1.10.0", "animation_controllers", Map.of(
                "controller.animation.hydraulic." + name, Map.of("initial_state", phases.get(0), "states", states)))).getAsJsonObject();
    }
    public static void configure(JsonObject description, JsonObject animations, String name, String prefix) {
        var all = animations.getAsJsonObject("animations");
        if (!name.equals("singer")) {
            JsonObject appear = all.getAsJsonObject(prefix + ".appear");
            all.add(prefix + ".close", reverse(appear));
            JsonObject open = appear.deepCopy();
            open.addProperty("anim_time_update", open.get("animation_length").getAsDouble());
            all.add(prefix + ".open", open);
        }
        Map<String, String> aliases = new LinkedHashMap<>();
        for (String alias : phases(name).values()) {
            String key = prefix + "." + alias;
            JsonObject animation = all.getAsJsonObject(key);
            if (animation == null) throw new IllegalStateException("Missing Sift phase animation " + key);
            if (!alias.equals("open")) {
                double length = animation.has("animation_length") ? animation.get("animation_length").getAsDouble() : 1;
                boolean loop = animation.has("loop") && animation.get("loop").isJsonPrimitive() && animation.get("loop").getAsJsonPrimitive().isBoolean() && animation.get("loop").getAsBoolean();
                animation.addProperty("anim_time_update", loop ? "math.mod(variable.hydraulic_time, " + length + ")" : "math.min(variable.hydraulic_time, " + length + ")");
            }
            aliases.put(alias, key);
        }
        aliases.put("sift_phase", "controller.animation.hydraulic." + name);
        description.add("animations", GSON.toJsonTree(aliases));
        description.add("scripts", GSON.toJsonTree(Map.of("initialize", List.of("variable.hydraulic_last_tick = -1; variable.hydraulic_last_variant = -1; variable.hydraulic_sync_life = 0; variable.hydraulic_time = 0;"),
                "pre_animation", List.of("(query.mark_variant != variable.hydraulic_last_tick || query.variant != variable.hydraulic_last_variant) ? { variable.hydraulic_sync_life = query.life_time; variable.hydraulic_last_tick = query.mark_variant; variable.hydraulic_last_variant = query.variant; }; variable.hydraulic_time = query.mark_variant / 20.0 + query.life_time - variable.hydraulic_sync_life;"), "animate", List.of("sift_phase"))));
    }
    public static JsonObject reverse(JsonObject source) {
        JsonObject result = source.deepCopy();
        double length = source.get("animation_length").getAsDouble();
        for (var bone : result.getAsJsonObject("bones").entrySet()) {
            var channels = bone.getValue().getAsJsonObject();
            for (String key : List.of("rotation", "position", "scale")) {
                if (!channels.has(key) || !channels.get(key).isJsonObject()) continue;
                JsonObject reversed = new JsonObject();
                for (var frame : channels.getAsJsonObject(key).entrySet()) {
                    reversed.add(Double.toString(Math.max(0, length - Double.parseDouble(frame.getKey()))), frame.getValue().deepCopy());
                }
                channels.add(key, reversed);
            }
        }
        return result;
    }
}
