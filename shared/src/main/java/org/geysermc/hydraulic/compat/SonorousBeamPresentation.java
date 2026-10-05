package org.geysermc.hydraulic.compat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;

/** One narrow, full-bright actor per original Sonorous beam; never a column of blocks. */
public final class SonorousBeamPresentation {
    public static final String IDENTIFIER = "hydraulic:sonorous_beam";
    public static final String GEOMETRY = "geometry.hydraulic.sonorous_beam";
    private static final Gson GSON = new Gson();
    public static JsonObject geometry() {
        return GSON.toJsonTree(Map.of("format_version", "1.16.0", "minecraft:geometry", List.of(Map.of(
                "description", Map.of("identifier", GEOMETRY, "texture_width", 16, "texture_height", 2048,
                        "visible_bounds_width", 2, "visible_bounds_height", 256, "visible_bounds_offset", List.of(0,64,0)),
                "bones", List.of(Map.of("name", "beam", "pivot", List.of(0,0,0), "cubes", List.of(Map.of(
                        "origin", List.of(-2.4,0,-2.4), "size", List.of(4.8,2048,4.8), "uv", Map.of(
                                "north", face(), "south", face(), "west", face(), "east", face()))))))))).getAsJsonObject();
    }
    private static Map<String,Object> face() { return Map.of("uv", List.of(0,0), "uv_size", List.of(16,2048)); }
    public static JsonObject entity() {
        return GSON.toJsonTree(Map.of("format_version", "1.10.0", "minecraft:client_entity", Map.of("description", Map.of(
                "identifier", IDENTIFIER, "materials", Map.of("default", "entity_alphablend"),
                "textures", Map.of("default", "textures/hydraulic/the_sift/sonorous_beam"),
                "geometry", Map.of("default", GEOMETRY), "animations", Map.of("growth", "animation.hydraulic.sonorous_beam"),
                "scripts", Map.of("animate", List.of("growth"), "pre_animation", List.of(
                        "variable.growth = variable.growth ?? 0; variable.growth = math.clamp(variable.growth + math.clamp(query.mark_variant / 1000 - variable.growth, -query.delta_time * 0.5, query.delta_time * 0.5), 0, 1);")), "render_controllers", List.of("controller.render.hydraulic.sonorous_beam"))))).getAsJsonObject();
    }
    public static JsonObject animation() {
        return GSON.toJsonTree(Map.of("format_version", "1.8.0", "animations", Map.of("animation.hydraulic.sonorous_beam", Map.of(
                "loop", true, "bones", Map.of("beam", Map.of("scale", List.of(1,"variable.growth",1))))))).getAsJsonObject();
    }
    public static JsonObject controller() {
        return GSON.toJsonTree(Map.of("format_version", "1.8.0", "render_controllers", Map.of("controller.render.hydraulic.sonorous_beam", Map.of(
                "geometry", "Geometry.default", "materials", List.of(Map.of("*", "Material.default")), "textures", List.of("Texture.default"),
                "uv_anim", Map.of("offset",List.of(0,"-math.mod(query.life_time,2) / 64"),"scale",List.of(1,"variable.growth")),
                "ignore_lighting", true, "is_hurt_color", Map.of("r",0,"g",0,"b",0,"a",0),
                "color", Map.of("r","math.mod(math.floor(query.variant / 65536),256) / 255",
                        "g","math.mod(math.floor(query.variant / 256),256) / 255", "b","math.mod(query.variant,256) / 255", "a",0.55))))).getAsJsonObject();
    }
}
