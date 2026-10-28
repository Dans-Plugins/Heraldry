package com.dansplugins.heraldry;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import static com.dansplugins.heraldry.MapRasteriser.MAP_SIZE;

/**
 * Draws a rasterised coat of arms onto a map.
 *
 * <p>The renderer is not contextual, so every player sees the same canvas. The server keeps that
 * canvas between renders, so the arms are drawn once rather than on every tick.
 */
public final class ArmsMapRenderer extends MapRenderer {

    private final byte[] pixels;
    private boolean drawn;

    /**
     * @param pixels {@code MAP_SIZE * MAP_SIZE} palette indices in row-major order, as returned by
     *               {@link MapRasteriser#rasterise}
     */
    public ArmsMapRenderer(byte[] pixels) {
        super(false);
        if (pixels == null || pixels.length != MAP_SIZE * MAP_SIZE) {
            throw new IllegalArgumentException("pixels must hold " + MAP_SIZE * MAP_SIZE + " palette indices, but held "
                    + (pixels == null ? "null" : pixels.length));
        }
        this.pixels = pixels.clone();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (drawn) {
            return;
        }
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                canvas.setPixel(x, y, pixels[y * MAP_SIZE + x]);
            }
        }
        drawn = true;
    }
}
