package org.geysermc.hydraulic.compat;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedTextureBudgetTest {
    @Test void animationKeepsFrameOrderCountAndAlpha() {
        var source = new BufferedImage(64, 192, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 192; y++) for (int x = 0; x < 64; x++) source.setRGB(x, y, 0x80123400 + y / 64);
        var result = AnimatedTextureBudget.reduce(source, 32);
        assertEquals(32, result.getWidth());
        assertEquals(96, result.getHeight());
        for (int frame = 0; frame < 3; frame++) {
            assertEquals(0x80123400 + frame, result.getRGB(0, frame * 32));
            assertEquals(0x80123400 + frame, result.getRGB(31, frame * 32 + 31));
        }
    }
    @Test void smallAndNonStripImagesRemainUnchanged() {
        var small = new BufferedImage(16, 512, BufferedImage.TYPE_INT_ARGB);
        assertSame(small, AnimatedTextureBudget.reduce(small, 32));
        var nonStrip = new BufferedImage(64, 100, BufferedImage.TYPE_INT_ARGB);
        assertSame(nonStrip, AnimatedTextureBudget.reduce(nonStrip, 32));
    }
}
