package org.geysermc.hydraulic.compat;

import org.junit.jupiter.api.Test;
import org.geysermc.hydraulic.block.IchorPresentation;
import org.geysermc.hydraulic.block.GeometryCulling;
import static org.junit.jupiter.api.Assertions.*;

class SiftEffectsPresentationTest {
    @Test void mistUsesTheDirectionModeInMojangsShriekParticle() {
        var components=SiftParticlePresentation.create("the_sift:ichor_surface_mist","mist",16,16)
                .getAsJsonObject("particle_effect").getAsJsonObject("components");
        var billboard=components.getAsJsonObject("minecraft:particle_appearance_billboard");
        assertEquals("direction_z",billboard.get("facing_camera_mode").getAsString());
        var direction=billboard.getAsJsonObject("direction");
        assertEquals("custom",direction.get("mode").getAsString());
        assertEquals(1,direction.getAsJsonArray("custom_direction").get(1).getAsInt());
    }
    @Test void recipeReservationSkipsBuiltinsWithoutReusingAlreadyAllocatedIds() {
        var next=new java.util.concurrent.atomic.AtomicInteger(1);
        RecipeNetworkIds.reserve(next,4);
        assertEquals(5,next.getAndIncrement());
        RecipeNetworkIds.reserve(next,4);
        assertEquals(6,next.getAndIncrement());
        RecipeNetworkIds.reserve(next,10);
        assertEquals(11,next.getAndIncrement());
    }

    @Test void flowingIchorRetainsNativeLevelsAndNeverCullsItsExposedSurface() {
        assertEquals(8/9f,IchorPresentation.height(0));
        assertEquals(1/9f,IchorPresentation.height(7));
        assertEquals(8/9f,IchorPresentation.height(15));
        for(int level=0;level<16;level++) {
            var geometry=IchorPresentation.geometry(level).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var cube=geometry.getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("cubes").get(0).getAsJsonObject();
            assertTrue(cube.getAsJsonArray("size").get(1).getAsFloat()>0);
            assertTrue(cube.getAsJsonArray("size").get(1).getAsFloat()<16);
            var rules=GeometryCulling.definition(geometry).getAsJsonObject("minecraft:block_culling_rules").getAsJsonArray("rules");
            for(var rule:rules) assertNotEquals("up",rule.getAsJsonObject().get("direction").getAsString());
        }
    }
    @Test void oneBeamRetainsOriginalWidthAndHeightWithoutGenerating128BlockActors() {
        var geometry=SonorousBeamPresentation.geometry().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        var bones=geometry.getAsJsonArray("bones");assertEquals(1,bones.size());
        var cubes=bones.get(0).getAsJsonObject().getAsJsonArray("cubes");assertEquals(1,cubes.size());
        var size=cubes.get(0).getAsJsonObject().getAsJsonArray("size");
        assertEquals(0.3,size.get(0).getAsDouble()/16,0.00001);assertEquals(128,size.get(1).getAsDouble()/16);
        assertEquals(4,cubes.get(0).getAsJsonObject().getAsJsonObject("uv").size());
    }
    @Test void originalNotesFadeInAndHaveFiniteLifetimeAndFragmentsFall() {
        var notes=SiftParticlePresentation.create("the_sift:sift_note","notes",16,16).getAsJsonObject("particle_effect").getAsJsonObject("components");
        assertTrue(notes.getAsJsonObject("minecraft:emitter_initialization").get("creation_expression").getAsString().contains("math.random(5,10)"));
        assertTrue(notes.getAsJsonObject("minecraft:particle_appearance_tinting").getAsJsonArray("color").get(3).getAsString().contains("/ 0.7"));
        var fragments=SiftParticlePresentation.create("the_sift:sift_parallax","fragments",32,32).getAsJsonObject("particle_effect").getAsJsonObject("components");
        assertEquals(-16,fragments.getAsJsonObject("minecraft:particle_motion_dynamic").getAsJsonArray("linear_acceleration").get(1).getAsInt());
        assertEquals(1,fragments.getAsJsonObject("minecraft:emitter_rate_instant").get("num_particles").getAsInt());
    }
}
