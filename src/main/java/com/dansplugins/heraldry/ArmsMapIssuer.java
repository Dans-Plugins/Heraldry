package com.dansplugins.heraldry;

import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Creates a new map that draws an owner's arms, and records it in {@link IssuedMaps} so
 * {@link MapRebinder} can draw the arms on it again after a restart.
 *
 * <p>Only the map view is handled here. Putting the view on a named, lore-tagged filled map item
 * needs a running server's item factory, and is left to the caller.
 */
public final class ArmsMapIssuer {

    /** Creates a new map view on the server, for example with {@code Bukkit.createMap(world)}. */
    @FunctionalInterface
    public interface MapFactory {
        MapView create();
    }

    private final ArmsStore store;
    private final MapRasteriser rasteriser;
    private final IssuedMaps issuedMaps;
    private final MapFactory factory;

    public ArmsMapIssuer(ArmsStore store, MapRasteriser rasteriser, IssuedMaps issuedMaps, MapFactory factory) {
        if (store == null || rasteriser == null || issuedMaps == null || factory == null) {
            throw new IllegalArgumentException("store, rasteriser, issuedMaps and factory must not be null");
        }
        this.store = store;
        this.rasteriser = rasteriser;
        this.issuedMaps = issuedMaps;
        this.factory = factory;
    }

    /**
     * Creates a map that draws the owner's arms and records the owner against its id.
     *
     * <p>The arms are read before the map is created, so an owner with no arms, or with arms that
     * cannot be read, leaves no unused map behind.
     *
     * @return the new map view, or empty if the owner has no arms
     * @throws IOException if the owner's arms cannot be read, or the new map cannot be recorded. In
     *                     the second case the map has been created and draws the arms until the
     *                     server restarts, but is not re-bound after that.
     */
    public Optional<MapView> issue(ArmsOwner owner) throws IOException {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        Optional<BufferedImage> arms = store.load(owner);
        if (!arms.isPresent()) {
            return Optional.empty();
        }
        ArmsMapRenderer renderer = new ArmsMapRenderer(rasteriser.rasterise(arms.get()));
        MapView view = factory.create();
        if (view == null) {
            throw new IllegalStateException("The map factory returned no map view");
        }
        for (MapRenderer existing : new ArrayList<>(view.getRenderers())) {
            view.removeRenderer(existing);
        }
        view.addRenderer(renderer);
        issuedMaps.record(view.getId(), owner);
        return Optional.of(view);
    }
}
