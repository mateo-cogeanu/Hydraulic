package org.geysermc.hydraulic.compat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.geysermc.pack.bedrock.resource.BedrockResourcePack;

/** Bound the decoded size of Sift's high-resolution fluid animation strips. */
public final class AnimatedTextureBudget {
    public static BufferedImage reduce(BufferedImage source, int maxFrameSize) {
        if (maxFrameSize < 1) throw new IllegalArgumentException("Invalid frame size");
        int width = source.getWidth();
        if (width <= maxFrameSize || source.getHeight() % width != 0) return source;
        int frames = source.getHeight() / width;
        BufferedImage result = new BufferedImage(maxFrameSize, maxFrameSize * frames, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < frames; frame++) {
            for (int y = 0; y < maxFrameSize; y++) {
                for (int x = 0; x < maxFrameSize; x++) {
                    result.setRGB(x, frame * maxFrameSize + y, source.getRGB(x * width / maxFrameSize,
                            frame * width + y * width / maxFrameSize));
                }
            }
        }
        return result;
    }
    public static int apply(BedrockResourcePack pack) throws IOException {
        int changed = 0;
        for (var flipbook : pack.flipbookTextures().values()) {
            var path = pack.directory().resolve(flipbook.flipbookTexture() + ".png");
            if (!Files.isRegularFile(path)) continue;
            BufferedImage source;
            try (var input = Files.newInputStream(path)) { source = ImageIO.read(input); }
            if (source == null) throw new IOException("Invalid animated texture: " + path);
            BufferedImage result = reduce(source, 32);
            if (result == source) continue;
            try (var output = Files.newOutputStream(path)) { ImageIO.write(result, "png", output); }
            changed++;
        }
        return changed;
    }
}
