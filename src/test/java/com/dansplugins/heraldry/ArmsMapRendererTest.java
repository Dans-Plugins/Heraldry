package com.dansplugins.heraldry;

import org.bukkit.map.MapCanvas;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static com.dansplugins.heraldry.MapRasteriser.MAP_SIZE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArmsMapRendererTest {

    @Test
    void everyPixelIsDrawnAtItsPosition() {
        byte[] pixels = distinctPixels();
        RecordingCanvas canvas = new RecordingCanvas();

        new ArmsMapRenderer(pixels).render(null, canvas.proxy(), null);

        assertArrayEquals(pixels, canvas.pixels);
        assertEquals(MAP_SIZE * MAP_SIZE, canvas.writes);
    }

    @Test
    void armsAreDrawnOnlyOnce() {
        RecordingCanvas canvas = new RecordingCanvas();
        ArmsMapRenderer renderer = new ArmsMapRenderer(distinctPixels());

        renderer.render(null, canvas.proxy(), null);
        renderer.render(null, canvas.proxy(), null);

        assertEquals(MAP_SIZE * MAP_SIZE, canvas.writes);
    }

    @Test
    void laterChangesToTheCallersArrayAreNotDrawn() {
        byte[] pixels = distinctPixels();
        byte[] expected = pixels.clone();
        ArmsMapRenderer renderer = new ArmsMapRenderer(pixels);
        pixels[0] = (byte) (pixels[0] + 1);
        RecordingCanvas canvas = new RecordingCanvas();

        renderer.render(null, canvas.proxy(), null);

        assertArrayEquals(expected, canvas.pixels);
    }

    @Test
    void rendererIsNotContextual() {
        assertFalse(new ArmsMapRenderer(distinctPixels()).isContextual());
    }

    @Test
    void wrongSizedPixelsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ArmsMapRenderer(new byte[MAP_SIZE]));
        assertThrows(IllegalArgumentException.class, () -> new ArmsMapRenderer(null));
    }

    /** Each pixel holds a value derived from its position, so a transposed or shifted draw is caught. */
    private static byte[] distinctPixels() {
        byte[] pixels = new byte[MAP_SIZE * MAP_SIZE];
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                pixels[y * MAP_SIZE + x] = (byte) (x * 3 + y * 7);
            }
        }
        return pixels;
    }

    /** A {@link MapCanvas} that records {@code setPixel} calls; no other method is expected. */
    private static final class RecordingCanvas {
        final byte[] pixels = new byte[MAP_SIZE * MAP_SIZE];
        int writes;

        MapCanvas proxy() {
            return (MapCanvas) Proxy.newProxyInstance(MapCanvas.class.getClassLoader(), new Class<?>[]{MapCanvas.class},
                    (proxy, method, args) -> {
                        if (!method.getName().equals("setPixel")) {
                            throw new UnsupportedOperationException(method.getName());
                        }
                        int x = (Integer) args[0];
                        int y = (Integer) args[1];
                        pixels[y * MAP_SIZE + x] = (Byte) args[2];
                        writes++;
                        return null;
                    });
        }
    }
}
