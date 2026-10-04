package org.geysermc.hydraulic.entity;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeckoAssetsTest {
    @Test void rewritesGeometryWithoutMutatingOriginal() {
        var source = JsonParser.parseString("{\"minecraft:geometry\":[{\"description\":{\"identifier\":\"geometry.unknown\"},\"bones\":[]}]} ").getAsJsonObject();
        var result = GeckoAssets.geometry(source, "geometry.hydraulic.singer");
        assertEquals("geometry.hydraulic.singer", result.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonObject("description").get("identifier").getAsString());
        assertEquals("geometry.unknown", source.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonObject("description").get("identifier").getAsString());
    }
    @Test void convertsGeckoVectorKeyframesToBedrockAndRemovesJavaCallbacks() {
        var source = JsonParser.parseString("""
            {"animations":{"walk":{"loop":true,"animation_length":1,"sound_effects":{"0":"java_callback"},
             "bones":{"leg":{"rotation":{"0":{"vector":[0,0,0]},"1":{"vector":[20,0,0],"easing":"easeInQuad"}},"scale":[1,1,1]}}}}}
            """).getAsJsonObject();
        var animation = GeckoAssets.animations(source, "animation.hydraulic.singer").getAsJsonObject("animations").getAsJsonObject("animation.hydraulic.singer.walk");
        assertTrue(animation.get("loop").getAsBoolean());
        assertFalse(animation.has("sound_effects"));
        assertEquals(20, animation.getAsJsonObject("bones").getAsJsonObject("leg").getAsJsonObject("rotation").getAsJsonArray("1").get(0).getAsInt());
        assertTrue(source.getAsJsonObject("animations").getAsJsonObject("walk").has("sound_effects"));
    }
}
