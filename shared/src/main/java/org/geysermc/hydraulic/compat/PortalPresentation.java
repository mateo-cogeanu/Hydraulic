package org.geysermc.hydraulic.compat;

import java.awt.image.BufferedImage;

/** An opaque, bounded atlas animation for the Java portal shader. */
public final class PortalPresentation {
    public static final String GEOMETRY = org.geysermc.hydraulic.block.StructureGeometry.IDENTIFIER;
    public static final int TEXTURE_SIZE = 64;
    public static final int FRAMES = 16;
    public static final int TICKS_PER_FRAME = 3;

    /** Bake The Sift 1.1.2 portal_fields.glsl at a neutral view into a 256 KiB atlas. */
    public static BufferedImage animation(BufferedImage source) {
        if (source == null) throw new IllegalArgumentException("Invalid portal image");
        BufferedImage strip = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE * FRAMES, BufferedImage.TYPE_INT_RGB);
        double period = FRAMES * TICKS_PER_FRAME / 20.0;
        for (int frame = 0; frame < FRAMES; frame++) {
            double seconds = frame * TICKS_PER_FRAME / 20.0;
            double blend = smoothstep(0, period, seconds);
            for (int y = 0; y < TEXTURE_SIZE; y++) for (int x = 0; x < TEXTURE_SIZE; x++) {
                double u = x / (double) (TEXTURE_SIZE - 1), v = y / (double) (TEXTURE_SIZE - 1);
                double[] color = mix(tiledField(u, v, seconds), tiledField(u, v, seconds - period), blend);
                int r = channel(color[0]), g = channel(color[1]), b = channel(color[2]);
                strip.setRGB(x, frame * TEXTURE_SIZE + y, 0xff000000 | r << 16 | g << 8 | b);
            }
        }
        return strip;
    }

    private static int channel(double value) { return Math.clamp((int) Math.round(value * 255), 0, 255); }
    private static double fract(double value) { return value - Math.floor(value); }
    private static double hash(double x, double y) { return fract(Math.sin(x * 127.1 + y * 311.7) * 43758.5453); }
    private static double mix(double a, double b, double t) { return a + (b - a) * t; }
    private static double[] mix(double[] a, double[] b, double t) {
        return new double[]{mix(a[0], b[0], t), mix(a[1], b[1], t), mix(a[2], b[2], t)};
    }
    private static double smoothstep(double low, double high, double value) {
        double t = Math.clamp((value - low) / (high - low), 0, 1);
        return t * t * (3 - 2 * t);
    }
    private static double noise(double x, double y) {
        double ix = Math.floor(x), iy = Math.floor(y), fx = fract(x), fy = fract(y);
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
        return mix(mix(hash(ix, iy), hash(ix + 1, iy), fx), mix(hash(ix, iy + 1), hash(ix + 1, iy + 1), fx), fy);
    }
    private static double fbm(double x, double y) {
        double result = 0, amplitude = 0.5;
        for (int i = 0; i < 5; i++) {
            result += amplitude * noise(x, y);
            double nextX = 1.6 * x + 1.2 * y;
            y = -1.2 * x + 1.6 * y; x = nextX; amplitude *= 0.5;
        }
        return result;
    }

    /** Original procedural colors, warp, octave weights and layer velocities; view offset is zero. */
    static double[] field(double u, double v, double seconds) {
        double x = u * 6, y = v * 4;
        double wx = fbm(x * .7 + seconds * .045, y * .7 + 3) - .5;
        double wy = fbm(x * .7 + 7, y * .7 - seconds * .036) - .5;
        double far = fbm(x * .75 + seconds * .055, y * .75 + seconds * .019);
        double middle = fbm(x * 1.35 + wx * 1.8 - seconds * .043, y * 1.35 + wy * 1.8 + seconds * .062);
        double near = fbm(x * 2.1 - wx + seconds * .032, y * 2.1 - wy - seconds * .049);
        double pockets = smoothstep(.38, .62, far * .35 + middle * .4 + near * .25);
        double wisps = smoothstep(.34, .66, middle) * smoothstep(.36, .63, near);
        double amount = Math.clamp(pockets * .95 + wisps * .35, 0, 1);
        double[] color = mix(new double[]{.94, .995, 1}, new double[]{.20, .66, .76}, amount);
        double haze = smoothstep(.5, .73, fbm(x * .55 + wx - seconds * .02, y * .55 + wy));
        return mix(color, new double[]{.99, 1, 1}, haze * .65);
    }

    private static double[] tiledField(double u, double v, double seconds) {
        // Match opposing edges without altering the shader field in the tile's center.
        double edgeU = .5 * (1 - smoothstep(0, .125, Math.min(u, 1 - u)));
        double edgeV = .5 * (1 - smoothstep(0, .125, Math.min(v, 1 - v)));
        double wrappedU = u + (u < .5 ? 1 : -1), wrappedV = v + (v < .5 ? 1 : -1);
        return mix(mix(field(u, v, seconds), field(wrappedU, v, seconds), edgeU),
                mix(field(u, wrappedV, seconds), field(wrappedU, wrappedV, seconds), edgeU), edgeV);
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
