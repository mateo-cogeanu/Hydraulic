package org.geysermc.hydraulic.compat;

import java.awt.image.BufferedImage;

/** An opaque, bounded atlas animation for the Java portal shader. */
public final class PortalPresentation {
    public static final String GEOMETRY = org.geysermc.hydraulic.block.StructureGeometry.IDENTIFIER;
    public static final int TEXTURE_SIZE = 32;
    public static final int FRAMES = 16;
    public static final int TICKS_PER_FRAME = 3;

    /** Small, seamless periodic sky animation; bounded to 64 KiB decoded RGB(A). */
    public static BufferedImage animation(BufferedImage source) {
        if (source == null) throw new IllegalArgumentException("Invalid portal image");
        BufferedImage strip = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE * FRAMES, BufferedImage.TYPE_INT_RGB);
        for (int frame = 0; frame < FRAMES; frame++) {
            double time = frame * Math.PI * 2 / FRAMES;
            for (int y = 0; y < TEXTURE_SIZE; y++) for (int x = 0; x < TEXTURE_SIZE; x++) {
                double u = x * Math.PI * 2 / TEXTURE_SIZE, v = y * Math.PI * 2 / TEXTURE_SIZE;
                double cloud = (Math.sin(u + time) + Math.cos(v - time) + Math.sin(u + v + 2 * time)) / 6 + 0.5;
                int tint = source.getRGB((int) (cloud * (source.getWidth() - 1)), source.getHeight() / 2);
                double star = Math.pow(Math.max(0, Math.sin(3 * u + time) * Math.cos(5 * v - time)), 28);
                int r = Math.clamp((int) (12 + ((tint >> 16) & 255) * cloud * 0.35 + star * 160), 0, 255);
                int g = Math.clamp((int) (18 + ((tint >> 8) & 255) * cloud * 0.45 + star * 190), 0, 255);
                int b = Math.clamp((int) (40 + (tint & 255) * cloud * 0.65 + star * 210), 0, 255);
                strip.setRGB(x, frame * TEXTURE_SIZE + y, 0xff000000 | r << 16 | g << 8 | b);
            }
        }
        return strip;
    }
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
