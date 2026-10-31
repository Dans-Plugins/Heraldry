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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.dansplugins.heraldry.MapRasteriser.MAP_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmsMapIssuerTest {

    private static final ArmsOwner PLAYER = ArmsOwner.player(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"));
    private static final ArmsOwner FACTION = ArmsOwner.faction("house_of_york");

    /** Red and blue arms rasterise to distinct, recognisable palette indices. */
    private static final MapRasteriser RASTERISER = new MapRasteriser(colour -> colour.getRed() > 0 ? (byte) 11 : (byte) 22);

    @TempDir
    Path root;

    @Test
    void theNewMapDrawsTheOwnersArmsAndIsRecorded() throws IOException {
        ArmsStore store = store();
        store.save(PLAYER, filled(Color.RED));
        store.save(FACTION, filled(Color.BLUE));
        IssuedMaps maps = maps();
        FakeFactory factory = new FakeFactory(7, 8);
        ArmsMapIssuer issuer = new ArmsMapIssuer(store, RASTERISER, maps, factory);

        Optional<MapView> first = issuer.issue(PLAYER);
        Optional<MapView> second = issuer.issue(FACTION);

        assertSame(factory.views.get(0).proxy, first.get());
        assertSame(factory.views.get(1).proxy, second.get());
        assertEquals(11, factory.views.get(0).drawnCentrePixel());
        assertEquals(22, factory.views.get(1).drawnCentrePixel());
        assertEquals(Optional.of(PLAYER), maps.ownerOf(7));
        assertEquals(Optional.of(FACTION), maps.ownerOf(8));
    }

    @Test
    void theNewMapsOwnRenderersAreReplaced() throws IOException {
        ArmsStore store = store();
        store.save(PLAYER, filled(Color.RED));
        FakeFactory factory = new FakeFactory(1);
        factory.terrain = true;

        new ArmsMapIssuer(store, RASTERISER, maps(), factory).issue(PLAYER);

        List<MapRenderer> renderers = factory.views.get(0).renderers;
        assertEquals(1, renderers.size());
        assertInstanceOf(ArmsMapRenderer.class, renderers.get(0));
    }

    @Test
    void theRecordSurvivesARestartAndTheMapIsReboundToTheOwnersArms() throws IOException {
        ArmsStore store = store();
        store.save(FACTION, filled(Color.BLUE));
        FakeFactory factory = new FakeFactory(3);
        new ArmsMapIssuer(store, RASTERISER, maps(), factory).issue(FACTION);
        FakeView view = factory.views.get(0);
        view.renderers.clear();

        MapRebinder.Result result = new MapRebinder(store, RASTERISER, id -> id == 3 ? view.proxy : null).rebind(maps());

        assertEquals(1, result.getRebound());
        assertEquals(22, view.drawnCentrePixel());
    }

    @Test
    void anOwnerWithNoArmsGetsNoMap() throws IOException {
        IssuedMaps maps = maps();
        FakeFactory factory = new FakeFactory(1);

        Optional<MapView> issued = new ArmsMapIssuer(store(), RASTERISER, maps, factory).issue(PLAYER);

        assertFalse(issued.isPresent());
        assertTrue(factory.views.isEmpty());
        assertEquals(Collections.emptyMap(), maps.all());
    }

    @Test
    void unreadableArmsCreateNoMap() throws IOException {
        ArmsStore store = store();
        Path file = store.fileFor(PLAYER);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{1, 2, 3});
        IssuedMaps maps = maps();
        FakeFactory factory = new FakeFactory(1);

        assertThrows(IOException.class, () -> new ArmsMapIssuer(store, RASTERISER, maps, factory).issue(PLAYER));

        assertTrue(factory.views.isEmpty());
        assertEquals(Collections.emptyMap(), maps.all());
    }

    @Test
    void aMapThatCannotBeRecordedIsReported() throws IOException {
        ArmsStore store = store();
        store.save(PLAYER, filled(Color.RED));
        // A non-empty directory where the issued maps file belongs cannot be replaced by it.
        Path blocked = root.resolve("issued-maps.txt");
        Files.createDirectories(blocked.resolve("occupied"));
        IssuedMaps maps = IssuedMaps.load(blocked);

        assertThrows(IOException.class,
                () -> new ArmsMapIssuer(store, RASTERISER, maps, new FakeFactory(1)).issue(PLAYER));

        assertEquals(Collections.emptyMap(), maps.all());
    }

    @Test
    void aFactoryThatCreatesNoMapIsReported() throws IOException {
        ArmsStore store = store();
        store.save(PLAYER, filled(Color.RED));
        IssuedMaps maps = maps();

        assertThrows(IllegalStateException.class,
                () -> new ArmsMapIssuer(store, RASTERISER, maps, () -> null).issue(PLAYER));

        assertEquals(Collections.emptyMap(), maps.all());
    }

    @Test
    void nullsAreRejected() throws IOException {
        ArmsStore store = store();
        IssuedMaps maps = maps();
        ArmsMapIssuer.MapFactory factory = new FakeFactory(1);
        assertThrows(IllegalArgumentException.class, () -> new ArmsMapIssuer(null, RASTERISER, maps, factory));
        assertThrows(IllegalArgumentException.class, () -> new ArmsMapIssuer(store, null, maps, factory));
        assertThrows(IllegalArgumentException.class, () -> new ArmsMapIssuer(store, RASTERISER, null, factory));
        assertThrows(IllegalArgumentException.class, () -> new ArmsMapIssuer(store, RASTERISER, maps, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ArmsMapIssuer(store, RASTERISER, maps, factory).issue(null));
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

    /** Hands out views with the given ids, in order, and keeps every view it created. */
    private static final class FakeFactory implements ArmsMapIssuer.MapFactory {
        final List<FakeView> views = new ArrayList<>();
        private final int[] ids;
        /** Whether new views start with a renderer of their own, as the server's do. */
        boolean terrain;

        FakeFactory(int... ids) {
            this.ids = ids;
        }

        @Override
        public MapView create() {
            FakeView view = new FakeView(ids[views.size()]);
            if (terrain) {
                view.renderers.add(new MapRenderer() {
                    @Override
                    public void render(MapView map, MapCanvas canvas, Player player) {
                    }
                });
            }
            views.add(view);
            return view.proxy;
        }
    }

    /** Keeps only the id and renderer list of a {@link MapView}; no other method is expected. */
    private static final class FakeView {
        final List<MapRenderer> renderers = new ArrayList<>();
        final MapView proxy;

        FakeView(int id) {
            proxy = (MapView) Proxy.newProxyInstance(MapView.class.getClassLoader(),
                    new Class<?>[]{MapView.class}, (self, method, args) -> {
                        switch (method.getName()) {
                            case "getId":
                                return id;
                            case "getRenderers":
                                return new ArrayList<>(renderers);
                            case "addRenderer":
                                renderers.add((MapRenderer) args[0]);
                                return null;
                            case "removeRenderer":
                                return renderers.remove((MapRenderer) args[0]);
                            case "hashCode":
                                return System.identityHashCode(self);
                            case "equals":
                                return self == args[0];
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }

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
