package org.geysermc.hydraulic.entity;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SiftAnimationControllersTest {
    @Test void singerTransitionsFollowNativeTimingBoundaries() {
        assertEquals(new SiftAnimationBridge.State(2, 147), SiftAnimationBridge.sequence(147, 148, 161));
        assertEquals(new SiftAnimationBridge.State(3, 0), SiftAnimationBridge.sequence(148, 148, 161));
        assertEquals(new SiftAnimationBridge.State(3, 160), SiftAnimationBridge.sequence(308, 148, 161));
        assertEquals(new SiftAnimationBridge.State(4, 0), SiftAnimationBridge.sequence(309, 148, 161));
    }
    @Test void allSingerPhasesAndRiftClosingHaveControllerTransitions() {
        var singer = SiftAnimationControllers.controller("singer").getAsJsonObject("animation_controllers")
                .getAsJsonObject("controller.animation.hydraulic.singer").getAsJsonObject("states");
        assertEquals(7, singer.size());
        assertEquals(6, singer.getAsJsonObject("appear").getAsJsonArray("transitions").size());
        var rift = SiftAnimationControllers.controller("rift").getAsJsonObject("animation_controllers")
                .getAsJsonObject("controller.animation.hydraulic.rift").getAsJsonObject("states");
        assertTrue(rift.has("open") && rift.has("appear") && rift.has("close"));
    }
    @Test void phaseAnimationsUseBedrockTimingAndConditionalSyntax() {
        var description = new com.google.gson.JsonObject();
        var animations = new com.google.gson.JsonObject();
        var definitions = new com.google.gson.JsonObject();
        String prefix = "animation.hydraulic.the_sift.singer";
        for (String phase : SiftAnimationControllers.phases("singer").values()) {
            definitions.add(prefix + "." + phase, JsonParser.parseString("{\"animation_length\":2,\"loop\":true,\"bones\":{}}"));
        }
        animations.add("animations", definitions);
        SiftAnimationControllers.configure(description, animations, "singer", prefix);
        for (var definition : definitions.entrySet()) {
            var animation = definition.getValue().getAsJsonObject();
            assertFalse(animation.has("anim_time"), "Unsupported field rejects Bedrock animations");
            assertTrue(animation.has("anim_time_update"));
        }
        String script = description.getAsJsonObject("scripts").getAsJsonArray("pre_animation").get(0).getAsString();
        assertFalse(script.contains("if ("), "Molang uses conditional operators, not Java if statements");
        assertTrue(script.contains("? {") && script.contains("};"));
    }
    @Test void closingReversesOpeningKeyframesWithoutChangingTheSource() {
        var source = JsonParser.parseString("{\"animation_length\":1,\"bones\":{\"main\":{\"scale\":{\"0.0\":[0,0,0],\"1.0\":[1,1,1]}}}}").getAsJsonObject();
        var reverse = SiftAnimationControllers.reverse(source);
        var scale = reverse.getAsJsonObject("bones").getAsJsonObject("main").getAsJsonObject("scale");
        assertEquals(1, scale.getAsJsonArray("0.0").get(0).getAsInt());
        assertEquals(0, scale.getAsJsonArray("1.0").get(0).getAsInt());
        assertEquals(0, source.getAsJsonObject("bones").getAsJsonObject("main").getAsJsonObject("scale").getAsJsonArray("0.0").get(0).getAsInt());
    }
}
