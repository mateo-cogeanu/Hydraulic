package org.geysermc.hydraulic.compat;

import java.awt.image.BufferedImage;

/** A static opaque atlas tile; never upload the Java shader's large rectangular texture. */
public final class PortalPresentation {
    public static final String GEOMETRY = "minecraft:geometry.full_block";
    public static final int TEXTURE_SIZE = 32;
    public static BufferedImage texture(BufferedImage source) {
        if (source == null) throw new IllegalArgumentException("Invalid portal image");
        BufferedImage result = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                result.setRGB(x, y, source.getRGB(x * source.getWidth() / TEXTURE_SIZE,
                        y * source.getHeight() / TEXTURE_SIZE) | 0xff000000);
            }
        }
        return result;
    }
    public static boolean isPortalEffect(String identifier) {
        return identifier.equals("the_sift:sift_parallax");
    }
}
