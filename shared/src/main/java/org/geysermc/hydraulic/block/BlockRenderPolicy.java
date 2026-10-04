package org.geysermc.hydraulic.block;

/** Java light occlusion does not identify textures that require alpha blending. */
public final class BlockRenderPolicy {
    public static String renderMethod(String identifier, boolean occludes, boolean cross) {
        if (cross) return "alpha_test_single_sided";
        if (occludes) return "opaque";
        if (identifier.startsWith("the_sift:") && !java.util.Set.of("the_sift:ichor_glass",
                "the_sift:ichor_glass_pane", "the_sift:ichor_cauldron").contains(identifier)) {
            return "alpha_test";
        }
        return "blend";
    }
}
