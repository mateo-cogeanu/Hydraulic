package org.geysermc.hydraulic.compat;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
class PortalPresentationTest {
    @Test void shaderFallbackBecomesSmallOpaqueSquare() {
        var source = new BufferedImage(384, 256, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0x00123456);
        var tile = PortalPresentation.texture(source);
        assertEquals(32, tile.getWidth());
        assertEquals(32, tile.getHeight());
        assertFalse(tile.getColorModel().hasAlpha());
        assertEquals(0xff123456, tile.getRGB(0, 0));
        assertEquals("minecraft:geometry.full_block", PortalPresentation.GEOMETRY);
    }
    @Test void portalEffectFilterPreservesOtherModEffects() {
        assertTrue(PortalPresentation.isPortalEffect("the_sift:sift_parallax"));
        assertFalse(PortalPresentation.isPortalEffect("the_sift:sift_note"));
        assertFalse(PortalPresentation.isPortalEffect("the_sift:singer_sound_wave"));
    }
}
