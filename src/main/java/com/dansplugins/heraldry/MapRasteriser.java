package com.dansplugins.heraldry;

import org.bukkit.map.MapPalette;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Rasterises an imported coat of arms to the 128x128 colour palette a Minecraft map uses.
 *
 * <p>The image is scaled to fit the map with its aspect ratio kept, centred, and padded with
 * transparent pixels. Each pixel is then replaced by the nearest map palette colour. Pixels that
 * are mostly transparent become {@link MapPalette#TRANSPARENT}, so the item frame shows through.
 */
public final class MapRasteriser {

    /** Width and height, in pixels, of a Minecraft map. */
    public static final int MAP_SIZE = 128;

    /** Alpha below which a pixel is treated as transparent. */
    static final int ALPHA_THRESHOLD = 128;

    /** Maps an opaque colour to a map palette index. */
    @FunctionalInterface
    public interface ColourMatcher {
        byte match(Color colour);
    }

    private final ColourMatcher matcher;

    /** Creates a rasteriser that matches colours against the server's map palette. */
    @SuppressWarnings("deprecation")
    public MapRasteriser() {
        this(MapPalette::matchColor);
    }

    public MapRasteriser(ColourMatcher matcher) {
        if (matcher == null) {
            throw new IllegalArgumentException("matcher must not be null");
        }
        this.matcher = matcher;
    }

    /**
     * Reads a PNG (or any other format ImageIO can decode) and rasterises it.
     *
     * @throws IllegalArgumentException if the stream is not a decodable image
     */
    public byte[] rasterise(InputStream in) throws IOException {
        BufferedImage image = ImageIO.read(in);
        if (image == null) {
            throw new IllegalArgumentException("Not a readable image; arms must be imported as a PNG.");
        }
        return rasterise(image);
    }

    /**
     * Rasterises an image to {@code MAP_SIZE * MAP_SIZE} palette indices, in row-major order.
     */
    public byte[] rasterise(BufferedImage image) {
        BufferedImage fitted = fitToMap(image);
        byte[] pixels = new byte[MAP_SIZE * MAP_SIZE];
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int argb = fitted.getRGB(x, y);
                if ((argb >>> 24) < ALPHA_THRESHOLD) {
                    pixels[y * MAP_SIZE + x] = MapPalette.TRANSPARENT;
                } else {
                    pixels[y * MAP_SIZE + x] = matcher.match(new Color(argb & 0xFFFFFF));
                }
            }
        }
        return pixels;
    }

    private static BufferedImage fitToMap(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double scale = Math.min((double) MAP_SIZE / width, (double) MAP_SIZE / height);
        int scaledWidth = Math.max(1, (int) Math.round(width * scale));
        int scaledHeight = Math.max(1, (int) Math.round(height * scale));
        int offsetX = (MAP_SIZE - scaledWidth) / 2;
        int offsetY = (MAP_SIZE - scaledHeight) / 2;

        BufferedImage fitted = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = fitted.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(image, offsetX, offsetY, scaledWidth, scaledHeight, null);
        } finally {
            graphics.dispose();
        }
        return fitted;
    }
}
