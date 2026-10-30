package com.dansplugins.heraldry;

import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adds an {@link ArmsMapRenderer} back to every issued map after a restart.
 *
 * <p>The server keeps map views between restarts but forgets their renderers. For each map recorded
 * in {@link IssuedMaps}, the owner's arms are loaded from the {@link ArmsStore} and rasterised, and the
 * map's renderers are replaced with one that draws them. Arms are loaded and rasterised once per
 * owner, and that owner's maps share one renderer.
 */
public final class MapRebinder {

    /** Finds the map view with an id, or returns null if the server has no such map. */
    @FunctionalInterface
    public interface MapLookup {
        MapView find(int mapId);
    }

    /** What a call to {@link #rebind} did. */
    public static final class Result {
        private final int rebound;
        private final List<String> problems;

        private Result(int rebound, List<String> problems) {
            this.rebound = rebound;
            this.problems = Collections.unmodifiableList(problems);
        }

        /** How many maps now draw their owner's arms. */
        public int getRebound() {
            return rebound;
        }

        /** One message per map that could not be re-bound, in ascending map id order. */
        public List<String> getProblems() {
            return problems;
        }
    }

    private final ArmsStore store;
    private final MapRasteriser rasteriser;
    private final MapLookup lookup;

    public MapRebinder(ArmsStore store, MapRasteriser rasteriser, MapLookup lookup) {
        if (store == null || rasteriser == null || lookup == null) {
            throw new IllegalArgumentException("store, rasteriser and lookup must not be null");
        }
        this.store = store;
        this.rasteriser = rasteriser;
        this.lookup = lookup;
    }

    /**
     * Re-binds every map in {@code maps}. A map that no longer exists, or whose owner has no readable
     * arms, is left as it is and reported in the result; the remaining maps are still re-bound.
     */
    public Result rebind(IssuedMaps maps) {
        if (maps == null) {
            throw new IllegalArgumentException("maps must not be null");
        }
        Map<ArmsOwner, ArmsMapRenderer> renderers = new HashMap<>();
        Map<ArmsOwner, String> ownerProblems = new HashMap<>();
        List<String> problems = new ArrayList<>();
        int rebound = 0;
        for (Map.Entry<Integer, ArmsOwner> entry : maps.all().entrySet()) {
            int mapId = entry.getKey();
            ArmsOwner owner = entry.getValue();
            MapView view = lookup.find(mapId);
            if (view == null) {
                problems.add("Map " + mapId + " of " + owner + " no longer exists on the server.");
                continue;
            }
            if (!renderers.containsKey(owner) && !ownerProblems.containsKey(owner)) {
                loadRenderer(owner, renderers, ownerProblems);
            }
            ArmsMapRenderer renderer = renderers.get(owner);
            if (renderer == null) {
                problems.add("Map " + mapId + " was not re-bound: " + ownerProblems.get(owner));
                continue;
            }
            for (MapRenderer existing : new ArrayList<>(view.getRenderers())) {
                view.removeRenderer(existing);
            }
            view.addRenderer(renderer);
            rebound++;
        }
        return new Result(rebound, problems);
    }

    private void loadRenderer(ArmsOwner owner, Map<ArmsOwner, ArmsMapRenderer> renderers,
                              Map<ArmsOwner, String> ownerProblems) {
        Optional<BufferedImage> arms;
        try {
            arms = store.load(owner);
        } catch (IOException e) {
            ownerProblems.put(owner, "the arms of " + owner + " could not be read: " + e.getMessage());
            return;
        }
        if (!arms.isPresent()) {
            ownerProblems.put(owner, owner + " has no arms.");
            return;
        }
        renderers.put(owner, new ArmsMapRenderer(rasteriser.rasterise(arms.get())));
    }
}
