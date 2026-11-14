package com.dansplugins.heraldry;

import org.bukkit.map.MapPalette;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.dansplugins.heraldry.MapRasteriser.MAP_SIZE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class MapRasteriserTest {

    private static final byte RED = MapPalette.matchColor(new Color(255, 0, 0));
    private static final byte WHITE = MapPalette.matchColor(Color.WHITE);

    @Test
    void flatTinctureFillsTheWholeMapWithOnePaletteColour() {
        byte[] pixels = new MapRasteriser().rasterise(filled(MAP_SIZE, MAP_SIZE, new Color(255, 0, 0, 255)));

        assertEquals(MAP_SIZE * MAP_SIZE, pixels.length);
        assertEquals(new Color(255, 0, 0), MapPalette.getColor(RED));
        for (byte pixel : pixels) {
            assertEquals(RED, pixel);
        }
    }

    @Test
    void transparentPixelsBecomeMapTransparent() {
        byte[] pixels = new MapRasteriser().rasterise(filled(MAP_SIZE, MAP_SIZE, new Color(255, 0, 0, 50)));

        for (byte pixel : pixels) {
            assertEquals(MapPalette.TRANSPARENT, pixel);
        }
    }

    @Test
    void mostlyOpaquePixelsAreKept() {
        byte[] pixels = new MapRasteriser().rasterise(filled(MAP_SIZE, MAP_SIZE, new Color(255, 0, 0, 200)));

        assertEquals(RED, pixels[at(64, 64)]);
    }

    @Test
    void largeImageIsScaledDownToTheMap() {
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 256, 512);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(256, 0, 256, 512);
        graphics.dispose();

        byte[] pixels = new MapRasteriser().rasterise(image);

        assertEquals(RED, pixels[at(10, 64)]);
        assertEquals(WHITE, pixels[at(117, 64)]);
    }

    @Test
    void tallImageKeepsItsAspectRatioAndIsCentred() {
        byte[] pixels = new MapRasteriser().rasterise(filled(256, 512, Color.RED));

        // 256x512 scales to 64x128, centred at x = 32..95.
        assertEquals(MapPalette.TRANSPARENT, pixels[at(0, 64)]);
        assertEquals(MapPalette.TRANSPARENT, pixels[at(30, 64)]);
        assertEquals(RED, pixels[at(34, 64)]);
        assertEquals(RED, pixels[at(64, 0)]);
        assertEquals(RED, pixels[at(64, 127)]);
        assertEquals(RED, pixels[at(93, 64)]);
        assertEquals(MapPalette.TRANSPARENT, pixels[at(97, 64)]);
        assertEquals(MapPalette.TRANSPARENT, pixels[at(127, 64)]);
    }

    @Test
    void pngStreamGivesTheSameResultAsTheDecodedImage() throws IOException {
        BufferedImage image = filled(40, 60, Color.WHITE);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);

        MapRasteriser rasteriser = new MapRasteriser();
        byte[] fromStream = rasteriser.rasterise(new ByteArrayInputStream(png.toByteArray()));

        assertArrayEquals(rasteriser.rasterise(image), fromStream);
        assertEquals(WHITE, fromStream[at(64, 64)]);
    }

    @Test
    void streamThatIsNotAnImageIsRejected() {
        byte[] notAnImage = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class,
                () -> new MapRasteriser().rasterise(new ByteArrayInputStream(notAnImage)));
    }

    @Test
    void suppliedMatcherDecidesTheOpaquePixels() {
        byte[] pixels = new MapRasteriser(colour -> (byte) 42).rasterise(filled(MAP_SIZE, MAP_SIZE, Color.BLUE));

        assertEquals(42, pixels[at(64, 64)]);
        assertNotEquals(MapPalette.TRANSPARENT, pixels[at(0, 0)]);
    }

    @Test
    void nullMatcherIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MapRasteriser(null));
    }

    @Test
    void alphaAtTheThresholdIsKeptAndJustBelowItIsTransparent() {
        MapRasteriser rasteriser = new MapRasteriser();

        byte[] atThreshold = rasteriser.rasterise(
                filled(MAP_SIZE, MAP_SIZE, new Color(255, 0, 0, MapRasteriser.ALPHA_THRESHOLD)));
        byte[] belowThreshold = rasteriser.rasterise(
                filled(MAP_SIZE, MAP_SIZE, new Color(255, 0, 0, MapRasteriser.ALPHA_THRESHOLD - 1)));

        assertEquals(RED, atThreshold[at(64, 64)]);
        assertEquals(MapPalette.TRANSPARENT, belowThreshold[at(64, 64)]);
    }

    @Test
    void smallImageIsScaledUpToFillTheMap() {
        byte[] pixels = new MapRasteriser().rasterise(filled(16, 16, Color.RED));

        for (byte pixel : pixels) {
            assertEquals(RED, pixel);
        }
    }

    @Test
    void wideImageKeepsItsAspectRatioAndIsCentredVertically() {
        byte[] pixels = new MapRasteriser().rasterise(filled(512, 256, Color.RED));

        // 512x256 scales to 128x64, centred at y = 32..95.
        assertEquals(MapPalette.TRANSPARENT, pixels[at(64, 0)]);
        assertEquals(MapPalette.TRANSPARENT, pixels[at(64, 30)]);
        assertEquals(RED, pixels[at(64, 34)]);
        assertEquals(RED, pixels[at(0, 64)]);
        assertEquals(RED, pixels[at(127, 64)]);
        assertEquals(RED, pixels[at(64, 93)]);
        assertEquals(MapPalette.TRANSPARENT, pixels[at(64, 97)]);
        assertEquals(MapPalette.TRANSPARENT, pixels[at(64, 127)]);
    }

    @Test
    void extremelyWideImageStillDrawsAtLeastOneRow() {
        // 2048x1 scales to 128x0.0625, which rounds to zero rows unless clamped to one.
        byte[] pixels = new MapRasteriser().rasterise(filled(2048, 1, Color.RED));

        // (128 - 1) / 2 = 63 is the single drawn row.
        assertEquals(RED, pixels[at(0, 63)]);
        assertEquals(RED, pixels[at(127, 63)]);
        assertEquals(MapPalette.TRANSPARENT, pixels[at(64, 62)]);
        assertEquals(MapPalette.TRANSPARENT, pixels[at(64, 64)]);
    }

    @Test
    void matcherReceivesOpaqueColoursAndIsNotAskedAboutTransparentPixels() {
        List<Color> seen = new ArrayList<>();
        MapRasteriser rasteriser = new MapRasteriser(colour -> {
            seen.add(colour);
            return (byte) 42;
        });

        rasteriser.rasterise(filled(MAP_SIZE, MAP_SIZE, new Color(10, 20, 30)));

        assertEquals(MAP_SIZE * MAP_SIZE, seen.size());
        assertEquals(new Color(10, 20, 30), seen.get(at(64, 64)));

        seen.clear();
        // Drawing onto the transparent canvas can shift a partly transparent colour by a step,
        // so only the alpha the matcher sees is asserted here.
        rasteriser.rasterise(filled(MAP_SIZE, MAP_SIZE, new Color(10, 20, 30, 200)));

        assertEquals(MAP_SIZE * MAP_SIZE, seen.size());
        for (Color colour : seen) {
            assertEquals(255, colour.getAlpha());
        }

        seen.clear();
        rasteriser.rasterise(filled(MAP_SIZE, MAP_SIZE, new Color(10, 20, 30, 0)));

        assertTrue(seen.isEmpty());
    }

    @Test
    void imageWithoutAnAlphaChannelIsTreatedAsOpaque() {
        BufferedImage opaque = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = opaque.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, MAP_SIZE, MAP_SIZE);
        graphics.dispose();

        byte[] pixels = new MapRasteriser().rasterise(opaque);

        for (byte pixel : pixels) {
            assertEquals(WHITE, pixel);
        }
    }

    private static BufferedImage filled(int width, int height, Color colour) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, colour.getRGB());
            }
        }
        return image;
    }

    private static int at(int x, int y) {
        return y * MAP_SIZE + x;
    }
}
