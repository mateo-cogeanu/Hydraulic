package org.geysermc.hydraulic.compat;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
class PortalPresentationTest {
    @Test void animatedSkyStaysSmallOpaqueAndChangesBetweenFrames() {
        var source = new BufferedImage(384, 256, BufferedImage.TYPE_INT_RGB);
        var strip = PortalPresentation.animation(source);
        assertEquals(64, strip.getWidth());
        assertEquals(1024, strip.getHeight());
        assertFalse(strip.getColorModel().hasAlpha());
        boolean differs = false;
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) differs |= strip.getRGB(x, y) != strip.getRGB(x, y + 64);
        assertTrue(differs);
    }
    @Test void shaderFallbackBecomesSmallOpaqueSquare() {
        var source = new BufferedImage(384, 256, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0x00123456);
        var tile = PortalPresentation.texture(source);
        assertEquals(64, tile.getWidth());
        assertEquals(64, tile.getHeight());
        assertFalse(tile.getColorModel().hasAlpha());
        assertEquals(0xff123456, tile.getRGB(0, 0));
        assertEquals(org.geysermc.hydraulic.block.StructureGeometry.IDENTIFIER, PortalPresentation.GEOMETRY);
    }
    @Test void portalEffectFilterPreservesOtherModEffects() {
        assertTrue(PortalPresentation.isPortalEffect("the_sift:sift_parallax"));
        assertFalse(PortalPresentation.isPortalEffect("the_sift:sift_note"));
        assertFalse(PortalPresentation.isPortalEffect("the_sift:singer_sound_wave"));
    }
    @Test void proceduralColorsMatchOriginalShaderReferenceAtNeutralView() {
        assertArrayEquals(new double[]{0.438182755716551,0.7678259772500603,0.837248461313476}, PortalPresentation.field(.2,.3,.45), 1e-8);
        assertArrayEquals(new double[]{0.939077098177492,0.9934979524033316,0.9987036713430644}, PortalPresentation.field(.5,.5,1.2), 1e-8);
    }
    @Test void atlasHasSeamlessEdgesAndOriginalWhiteTurquoisePalette() {
        var atlas = PortalPresentation.animation(new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB));
        for(int frame=0;frame<PortalPresentation.FRAMES;frame++) {
            int offset=frame*PortalPresentation.TEXTURE_SIZE;
            for(int i=0;i<PortalPresentation.TEXTURE_SIZE;i++) {
                assertEquals(atlas.getRGB(0,offset+i),atlas.getRGB(PortalPresentation.TEXTURE_SIZE-1,offset+i));
                assertEquals(atlas.getRGB(i,offset),atlas.getRGB(i,offset+PortalPresentation.TEXTURE_SIZE-1));
            }
        }
        for(int y=0;y<atlas.getHeight();y++) for(int x=0;x<atlas.getWidth();x++) {
            int rgb=atlas.getRGB(x,y),r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
            assertTrue(r>=51 && g>=168 && b>=194);
            assertTrue(r<=g && g<=b);
        }
    }

}
