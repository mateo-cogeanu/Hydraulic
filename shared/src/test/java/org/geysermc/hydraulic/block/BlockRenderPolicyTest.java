package org.geysermc.hydraulic.block;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BlockRenderPolicyTest {
    @Test void denseVegetationUsesCutoutInsteadOfBlending() {
        for (String id : java.util.List.of("overgrown_willow_vines", "overgrown_willow_vines_plant", "overgrown_willow_foliage", "sunburst_plant", "overgrown_lotus"))
            assertEquals("alpha_test", BlockRenderPolicy.renderMethod("the_sift:" + id, false, false));
    }
    @Test void lightPassingWoodDoesNotBecomeTranslucent() {
        assertEquals("alpha_test", BlockRenderPolicy.renderMethod("the_sift:overgrown_willow_door", false, false));
        assertEquals("opaque", BlockRenderPolicy.renderMethod("the_sift:siftslate", true, false));
    }
    @Test void glassAndFluidSurfacesRetainBlending() {
        for (String id : java.util.List.of("ichor_glass", "ichor_glass_pane", "ichor_cauldron"))
            assertEquals("blend", BlockRenderPolicy.renderMethod("the_sift:" + id, false, false));
    }
    @Test void crossPlantsAndOtherModsKeepExistingPolicy() {
        assertEquals("alpha_test_single_sided", BlockRenderPolicy.renderMethod("the_sift:sculkflower", false, true));
        assertEquals("blend", BlockRenderPolicy.renderMethod("other:glass", false, false));
        assertTrue(StructureGeometry.isSiftSolidCube("the_sift:dry_healthy_sculk"));
        assertFalse(StructureGeometry.isSiftSolidCube("the_sift:overgrown_willow_vines"));
    }
}
