package org.geysermc.hydraulic.compat;

import com.google.auto.service.AutoService;
import com.google.gson.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.core.registries.Registries;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.pack.PackModule;

/** Give original Sift biomes their own client IDs so colors never overwrite Overworld biomes. */
@AutoService(PackModule.class)
public class SiftBiomes extends PackModule<SiftBiomes> {
    private static final Gson GSON = new Gson();
    // Bedrock reserves the signed-short range starting at 30000 for custom biomes.
    public static final int FIRST_CUSTOM_ID = 30000;
    public static final Map<String,Integer> IDS = new LinkedHashMap<>();
    public SiftBiomes() {
        listenOn(GeyserDefineEntitiesEvent.class,context->{
            if(!context.mod().namespace().equals("the_sift"))return;
            var registry=org.geysermc.geyser.registry.Registries.BIOME_IDENTIFIERS.get();
            var definitions=new LinkedHashMap<>(org.geysermc.geyser.registry.Registries.BIOMES.get().getDefinitions());
            var original=definitions.get("minecraft:ocean");
            int next=Math.max(FIRST_CUSTOM_ID,registry.values().intStream().max().orElse(0)+1);
            for(var key:HydraulicImpl.instance().server().registryAccess().lookupOrThrow(Registries.BIOME).keySet()) {
                if(!key.getNamespace().equals("the_sift"))continue;
                String name=key.toString();int id=registry.containsKey(name)?registry.getInt(name):next++;
                IDS.put(name,id);registry.put(name,id);
                JsonObject climate;
                try { climate=JsonParser.parseString(Files.readString(context.mod().resolveFile("data/the_sift/worldgen/biome/"+key.getPath()+".json"))).getAsJsonObject(); }
                catch(java.io.IOException e) { throw new IllegalStateException("Cannot read Sift climate",e); }
                // Proxied worlds do not use client-side biome world generation.
                // Leave that optional nested payload absent, as in Geyser custom biome support.
                definitions.put(name,new BiomeDefinitionData(id,climate.get("temperature").getAsFloat(),climate.get("downfall").getAsFloat(),
                        0f,0f,0f,0f,0f,original.getDepth(),original.getScale(),original.getMapWaterColor(),climate.get("has_precipitation").getAsBoolean(),
                        List.of("the_sift"),null));
            }
            org.geysermc.geyser.registry.Registries.BIOMES.set(new BiomeDefinitions(definitions));
            context.logger().info("Registered {} dedicated Sift biome IDs",IDS.size());
        });
        postProcess(context->{
            if(!context.mod().namespace().equals("the_sift"))return;
            for(String name:IDS.keySet()) {
                String path=name.substring(name.indexOf(':')+1);
                try {
                    var original=JsonParser.parseString(Files.readString(context.mod().resolveFile("data/the_sift/worldgen/biome/"+path+".json"))).getAsJsonObject();
                    var attributes=original.getAsJsonObject("attributes");
                    String fog=attributes.has("minecraft:visual/fog_color")?attributes.get("minecraft:visual/fog_color").getAsString():"#efc7e0";
                    // Original renderer's daytime zenith (0.43,0.87,0.94), rather than its white shader input.
                    var components=Map.of("minecraft:sky_color",Map.of("sky_color","#6edeee"),
                            "minecraft:fog_appearance",Map.of("fog_identifier",name),
                            "minecraft:water_appearance",Map.of("surface_color",attributes.has("minecraft:visual/water_color")?attributes.get("minecraft:visual/water_color").getAsString():"#43bccd"));
                    context.bedrockResourcePack().addExtraFile(GSON.toJsonTree(Map.of("format_version","1.21.40","minecraft:client_biome",Map.of(
                            "description",Map.of("identifier",name),"components",components))),"biomes/"+path+".client_biome.json");
                    context.bedrockResourcePack().addExtraFile(GSON.toJsonTree(Map.of("format_version","1.16.100","minecraft:fog_settings",Map.of(
                            "description",Map.of("identifier",name),"distance",Map.of("air",Map.of("fog_start",0.75,"fog_end",1.0,"fog_color",fog,"render_distance_type","render"))))),"fogs/"+path+".json");
                } catch(java.io.IOException e) {throw new IllegalStateException("Cannot convert Sift biome "+name,e);}
            }
        });
    }
}
