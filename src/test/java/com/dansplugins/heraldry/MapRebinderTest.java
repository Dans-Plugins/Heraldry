package com.dansplugins.heraldry;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.dansplugins.heraldry.MapRasteriser.MAP_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapRebinderTest {

    private static final ArmsOwner PLAYER = ArmsOwner.player(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"));
    private static final ArmsOwner FACTION = ArmsOwner.faction("house_of_york");

    /** Red and blue arms rasterise to distinct, recognisable palette indices. */
    private static final MapRasteriser RASTERISER = new MapRasteriser(colour -> colour.getRed() > 0 ? (byte) 11 : (byte) 22);

    @TempDir
    Path root;

    @Test
    void eachMapDrawsItsOwnersArms() throws IOException {
        ArmsStore store = store();
        store.save(PLAYER, filled(Color.RED));
        store.save(FACTION, filled(Color.BLUE));
        IssuedMaps maps = maps();
        maps.record(1, PLAYER);
        maps.record(2, FACTION);
        Map<Integer, FakeView> views = views(1, 2);

        MapRebinder.Result result = new MapRebinder(store, RASTERISER, lookup(views)).rebind(maps);

        assertEquals(2, result.getRebound());
        assertEquals(Collections.emptyList(), result.getProblems());
        assertEquals(11, views.get(1).drawnCentrePixel());
        assertEquals(22, views.get(2).drawnCentrePixel());
    }

    @Test
    void existingRenderersAreReplaced() throws IOException {
        ArmsStore store = store();
        store.save(PLAYER, filled(Color.RED));
        IssuedMaps maps = maps();
        maps.record(1, PLAYER);
        Map<Integer, FakeView> views = views(1);
        MapRenderer terrain = new MapRenderer() {
            @Override
            public void render(MapView view, MapCanvas canvas, Player player) {
            }
        };
        views.get(1).renderers.add(terrain);

        new MapRebinder(store, RASTERISER, lookup(views)).rebind(maps);

        assertEquals(1, views.get(1).renderers.size());
        assertInstanceOf(ArmsMapRenderer.class, views.get(1).renderers.get(0));
    }

    @Test
    void mapsOfOneOwnerShareARenderer() throws IOException {
        ArmsStore store = store();
        store.save(FACTION, filled(Color.BLUE));
        IssuedMaps maps = maps();
        maps.record(1, FACTION);
        maps.record(2, FACTION);
        Map<Integer, FakeView> views = views(1, 2);

        new MapRebinder(store, RASTERISER, lookup(views)).rebind(maps);

        assertSame(views.get(1).renderers.get(0), views.get(2).renderers.get(0));
    }

    @Test
    void aMapThatNoLongerExistsIsReportedAndTheRestAreStillRebound() throws IOException {
        ArmsStore store = store();
        store.save(PLAYER, filled(Color.RED));
        IssuedMaps maps = maps();
        maps.record(1, PLAYER);
        maps.record(2, PLAYER);
        Map<Integer, FakeView> views = views(2);

        MapRebinder.Result result = new MapRebinder(store, RASTERISER, lookup(views)).rebind(maps);

        assertEquals(1, result.getRebound());
        assertEquals(Collections.singletonList("Map 1 of " + PLAYER + " no longer exists on the server."),
                result.getProblems());
        assertEquals(11, views.get(2).drawnCentrePixel());
    }

    @Test
    void aMapWhoseOwnerHasNoArmsIsReportedAndLeftAlone() throws IOException {
        IssuedMaps maps = maps();
        maps.record(1, FACTION);
        maps.record(2, FACTION);
        Map<Integer, FakeView> views = views(1, 2);

        MapRebinder.Result result = new MapRebinder(store(), RASTERISER, lookup(views)).rebind(maps);

        assertEquals(0, result.getRebound());
        assertEquals(Arrays.asList(
                "Map 1 was not re-bound: " + FACTION + " has no arms.",
                "Map 2 was not re-bound: " + FACTION + " has no arms."), result.getProblems());
        assertTrue(views.get(1).renderers.isEmpty());
    }

    @Test
    void unreadableArmsAreReported() throws IOException {
        ArmsStore store = store();
        Path file = store.fileFor(PLAYER);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{1, 2, 3});
        IssuedMaps maps = maps();
        maps.record(1, PLAYER);

        MapRebinder.Result result = new MapRebinder(store, RASTERISER, lookup(views(1))).rebind(maps);

        assertEquals(0, result.getRebound());
        assertEquals(1, result.getProblems().size());
        assertTrue(result.getProblems().get(0).startsWith("Map 1 was not re-bound: the arms of " + PLAYER
                + " could not be read: "), result.getProblems().get(0));
    }

    @Test
    void nullsAreRejected() throws IOException {
        ArmsStore store = store();
        assertThrows(IllegalArgumentException.class, () -> new MapRebinder(null, RASTERISER, id -> null));
        assertThrows(IllegalArgumentException.class, () -> new MapRebinder(store, null, id -> null));
        assertThrows(IllegalArgumentException.class, () -> new MapRebinder(store, RASTERISER, null));
        assertThrows(IllegalArgumentException.class, () -> new MapRebinder(store, RASTERISER, id -> null).rebind(null));
    }

    private ArmsStore store() {
        return new ArmsStore(root.resolve("arms"));
    }

    private IssuedMaps maps() throws IOException {
        return IssuedMaps.load(root.resolve("issued-maps.txt"));
    }

    private static BufferedImage filled(Color colour) {
        BufferedImage image = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                image.setRGB(x, y, colour.getRGB());
            }
        }
        return image;
    }

    private static Map<Integer, FakeView> views(int... ids) {
        Map<Integer, FakeView> views = new HashMap<>();
        for (int id : ids) {
            views.put(id, new FakeView());
        }
        return views;
    }

    private static MapRebinder.MapLookup lookup(Map<Integer, FakeView> views) {
        return id -> views.containsKey(id) ? views.get(id).proxy : null;
    }

    /** Keeps only the renderer list of a {@link MapView}; no other method is expected. */
    private static final class FakeView {
        final List<MapRenderer> renderers = new ArrayList<>();
        final MapView proxy = (MapView) Proxy.newProxyInstance(MapView.class.getClassLoader(),
                new Class<?>[]{MapView.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getRenderers":
                            return new ArrayList<>(renderers);
                        case "addRenderer":
                            renderers.add((MapRenderer) args[0]);
                            return null;
                        case "removeRenderer":
                            return renderers.remove((MapRenderer) args[0]);
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });

        /** Renders with the only renderer and returns the palette index it drew at the map's centre. */
        byte drawnCentrePixel() {
            assertEquals(1, renderers.size());
            byte[] drawn = new byte[1];
            MapCanvas canvas = (MapCanvas) Proxy.newProxyInstance(MapCanvas.class.getClassLoader(),
                    new Class<?>[]{MapCanvas.class}, (canvasProxy, method, args) -> {
                        if ((Integer) args[0] == MAP_SIZE / 2 && (Integer) args[1] == MAP_SIZE / 2) {
                            drawn[0] = (Byte) args[2];
                        }
                        return null;
                    });
            renderers.get(0).render(proxy, canvas, null);
            return drawn[0];
        }
    }
}
