package org.geysermc.hydraulic.compat;

import com.google.auto.service.AutoService;
import com.google.gson.*;
import org.geysermc.hydraulic.pack.PackModule;
import java.nio.file.Files;
import java.util.Map;
import java.util.List;
import javax.imageio.ImageIO;

/** Bedrock presentations for effects that the Java mod renders entirely in client code. */
@AutoService(PackModule.class)
public class SiftVisuals extends PackModule<SiftVisuals> {
    private static final Gson GSON = new Gson();
    public SiftVisuals() {
        preProcess(context -> {
            if (!context.mod().namespace().equals("the_sift")) return;
            for (String name : List.of("sift_parallax", "sift_note", "sound_wave", "singer_sound_wave", "canyon_soul", "soul_fragment",
                    "ichor_surface_mist", "ichor_bubble", "ichor_splash", "ichor_rain_splash")) NativePacketBridge.PARTICLES.add("the_sift:" + name);
        });
        postProcess(context -> {
            if (!context.mod().namespace().equals("the_sift")) return;
            try {
                var pack = context.bedrockResourcePack();
                var image = context.mod().resolveFile("assets/the_sift/textures/misc/sift_portal_shaderpack.png");
                if (image == null) image = context.mod().resolveFile("assets/the_sift/textures/block/sift_portal_mist.png");
                pack.addExtraFile(Files.readAllBytes(image), "textures/hydraulic/the_sift/portal.png");
                pack.addBlockTexture("hydraulic_sift_portal", "textures/hydraulic/the_sift/portal");
                pack.addExtraFile(JsonParser.parseString("""
                        {"format_version":"1.16.0","minecraft:geometry":[{
                          "description":{"identifier":"geometry.hydraulic.sift_portal","texture_width":16,"texture_height":16},
                          "bones":[{"name":"portal","pivot":[0,8,0],"cubes":[{
                            "origin":[-8,0,-0.05],"size":[16,16,0.1],"uv":{
                              "north":{"uv":[0,0],"uv_size":[16,16]},
                              "south":{"uv":[16,0],"uv_size":[-16,16]}}}]}]}]}
                        """), "models/blocks/hydraulic_sift_portal.geo.json");
                for (String identifier : NativePacketBridge.PARTICLES) {
                    String name = identifier.split(":")[1];
                    var source = context.mod().resolveFile("assets/the_sift/particles/" + name + ".json");
                    var textureKey = JsonParser.parseString(Files.readString(source)).getAsJsonObject().getAsJsonArray("textures").get(0).getAsString();
                    String texture = textureKey.substring(textureKey.indexOf(':') + 1);
                    var textureFile = context.mod().resolveFile("assets/the_sift/textures/particle/" + texture + ".png");
                    if (textureFile == null) textureFile = context.mod().resolveFile("assets/the_sift/textures/particle/sift_portal_mist.png");
                    java.awt.image.BufferedImage bitmap;
                    try (var input = Files.newInputStream(textureFile)) { bitmap = ImageIO.read(input); }
                    String output = "textures/hydraulic/the_sift/particle/" + name;
                    pack.addExtraFile(Files.readAllBytes(textureFile), output + ".png");
                    boolean wave = name.contains("sound_wave");
                    var components = Map.of(
                            "minecraft:emitter_rate_instant", Map.of("num_particles", 1),
                            "minecraft:emitter_lifetime_once", Map.of("active_time", 0.01),
                            "minecraft:emitter_shape_point", Map.of("offset", List.of(0, 0, 0), "direction", List.of(0, 1, 0)),
                            "minecraft:particle_initial_speed", wave ? 0 : 0.15,
                            "minecraft:particle_lifetime_expression", Map.of("max_lifetime", wave ? 0.5 : 1.2),
                            "minecraft:particle_motion_dynamic", Map.of("linear_acceleration", List.of(0, 0.1, 0)),
                            "minecraft:particle_appearance_billboard", Map.of("size", wave ? List.of("0.1 + variable.particle_age * 2", "0.1 + variable.particle_age * 2") : List.of(0.12, 0.12),
                                    "facing_camera_mode", "lookat_xyz", "uv", Map.of("texture_width", bitmap.getWidth(), "texture_height", bitmap.getHeight(), "uv", List.of(0, 0), "uv_size", List.of(bitmap.getWidth(), bitmap.getHeight()))),
                            "minecraft:particle_appearance_tinting", Map.of("color", List.of(1, 1, 1, "1 - variable.particle_age / variable.particle_lifetime")));
                    pack.addExtraFile(GSON.toJsonTree(Map.of("format_version", "1.10.0", "particle_effect", Map.of("description", Map.of(
                            "identifier", identifier, "basic_render_parameters", Map.of("material", "particles_blend", "texture", output)), "components", components))), "particles/" + name + ".json");
                }
            } catch (Exception e) { throw new IllegalStateException("Could not convert Sift portal/effects", e); }
        });
    }
}
