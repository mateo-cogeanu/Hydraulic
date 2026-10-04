package org.geysermc.hydraulic.compat;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
class PortalPresentationTest {
    @Test void animatedSkyStaysSmallOpaqueAndChangesBetweenFrames() {
        var source = new BufferedImage(384, 256, BufferedImage.TYPE_INT_RGB);
        var strip = PortalPresentation.animation(source);
        assertEquals(32, strip.getWidth());
        assertEquals(512, strip.getHeight());
        assertFalse(strip.getColorModel().hasAlpha());
        boolean differs = false;
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) differs |= strip.getRGB(x, y) != strip.getRGB(x, y + 32);
        assertTrue(differs);
    }
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
