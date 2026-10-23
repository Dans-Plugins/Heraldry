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

import static com.dansplugins.heraldry.MapRasteriser.MAP_SIZE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
