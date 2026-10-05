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
                pack.addExtraFile(SonorousBeamPresentation.geometry(), "models/entity/sonorous_beam.geo.json");
                pack.addExtraFile(SonorousBeamPresentation.entity(), "entity/sonorous_beam.entity.json");
                pack.addExtraFile(SonorousBeamPresentation.animation(), "animations/sonorous_beam.animation.json");
                pack.addExtraFile(SonorousBeamPresentation.controller(), "render_controllers/sonorous_beam.json");
                // Tile the original beam texture in one bounded atlas; one actor, four faces.
                java.awt.image.BufferedImage beamSource;
                try (var input = Files.newInputStream(context.mod().resolveFile("assets/the_sift/textures/block/sonorous_deepslate_beam.png"))) { beamSource = ImageIO.read(input); }
                var beam = new java.awt.image.BufferedImage(16,2048,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                for (int y=0;y<2048;y++) for(int x=0;x<16;x++) beam.setRGB(x,y,beamSource.getRGB(x*beamSource.getWidth()/16,(y%32)*beamSource.getHeight()/32));
                var beamBytes = new java.io.ByteArrayOutputStream(); ImageIO.write(beam,"png",beamBytes);
                pack.addExtraFile(beamBytes.toByteArray(),"textures/hydraulic/the_sift/sonorous_beam.png");
                var image = context.mod().resolveFile("assets/the_sift/textures/misc/sift_portal_shaderpack.png");
                if (image == null) image = context.mod().resolveFile("assets/the_sift/textures/block/sift_portal_mist.png");
                java.awt.image.BufferedImage portal;
                try (var input = Files.newInputStream(image)) { portal = PortalPresentation.animation(ImageIO.read(input)); }
                var encoded = new java.io.ByteArrayOutputStream();
                ImageIO.write(portal, "png", encoded);
                pack.addExtraFile(encoded.toByteArray(), "textures/hydraulic/the_sift/portal.png");
                pack.addBlockTexture("hydraulic_sift_portal", "textures/hydraulic/the_sift/portal");
                pack.addFlipbookTexture("hydraulic_sift_portal", "textures/hydraulic/the_sift/portal", PortalPresentation.TICKS_PER_FRAME);
                pack.addExtraFile(GSON.toJsonTree(Map.of("format_version", "1.16.100", "minecraft:texture_set", Map.of(
                        "color", "portal", "metalness_emissive_roughness", List.of(0, 255, 255)))), "textures/hydraulic/the_sift/portal.texture_set.json");
                pack.addExtraFile(org.geysermc.hydraulic.block.StructureGeometry.culling(), "block_culling/hydraulic_solid_cube.json");
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
                    pack.addExtraFile(SiftParticlePresentation.create(identifier, output, bitmap.getWidth(), bitmap.getHeight()), "particles/" + name + ".json");
                }
            } catch (Exception e) { throw new IllegalStateException("Could not convert Sift portal/effects", e); }
        });
    }
}
