package com.dansplugins.heraldry;

import org.bukkit.command.CommandSender;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeraldryCommandTest {

    private static final String[] ALL_PERMISSIONS = {"heraldry.view", "heraldry.set", "heraldry.issue"};

    /** Red and blue arms rasterise to distinct, recognisable palette indices. */
    private static final byte RED_PIXEL = 11;
    private static final byte BLUE_PIXEL = 22;

    @TempDir
    Path dataFolder;

    private ArmsStore store;
    private IssuedMaps maps;
    private FakeEnvironment environment;
    private HeraldryCommand command;
    private List<String> logged;
    private FakePlayer steve;
    private FakePlayer alex;

    @BeforeEach
    void setUp() throws IOException {
        store = new ArmsStore(dataFolder.resolve("arms"));
        maps = IssuedMaps.load(dataFolder.resolve("issued-maps.txt"));
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        writePng(imports.resolve("york.png"), Color.RED);
        writePng(imports.resolve("lancaster.png"), Color.BLUE);

        logged = new ArrayList<>();
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        logger.setLevel(Level.ALL);

        environment = new FakeEnvironment();
        MapRasteriser rasteriser = new MapRasteriser(colour -> colour.getRed() > 0 ? RED_PIXEL : BLUE_PIXEL);
        MapRebinder rebinder = new MapRebinder(store, rasteriser, environment::findMap);
        command = new HeraldryCommand(new ArmsImporter(imports, store), store, rasteriser, maps, rebinder,
                environment, logger);
        steve = new FakePlayer("Steve", "0f8fad5b-d9cb-469f-a165-70867728950e", ALL_PERMISSIONS);
        alex = new FakePlayer("Alex", "7c9e6679-7425-40de-944b-e07fc1f90ae7");
    }

    @Test
    void noArgumentsOrAnUnknownSubcommandShowsTheUsage() {
        run(steve.proxy);
        run(steve.proxy, "burn");

        assertEquals(2, steve.messages.size());
        for (String message : steve.messages) {
            for (String subcommand : HeraldryCommand.SUBCOMMANDS) {
                assertTrue(message.contains("/heraldry " + subcommand), message);
            }
        }
    }

    @Test
    void subcommandsAreCaseInsensitive() {
        run(steve.proxy, "SET", "york.png");

        assertTrue(store.has(owner(steve)));
    }

    @Test
    void eachSubcommandNeedsItsOwnPermission() {
        store(alex, Color.RED);
        FakePlayer onlySet = new FakePlayer("Sam", "c1b5a2f0-3a6e-4d1f-9d0e-2a7b8c9d0e1f", "heraldry.set");
        FakePlayer onlyIssue = new FakePlayer("Ida", "d2c6b3e1-4b7f-4e2a-8e1f-3b8c9d0e1f2a", "heraldry.issue");

        run(alex.proxy, "set", "york.png");
        run(alex.proxy, "issue");
        run(alex.proxy, "view");
        run(onlyIssue.proxy, "set", "york.png");
        run(onlySet.proxy, "issue");
        run(onlySet.proxy, "view");

        assertEquals(3, alex.messages.size());
        assertTrue(alex.last().contains("permission"), alex.last());
        assertTrue(onlyIssue.last().contains("permission"), onlyIssue.last());
        assertTrue(onlySet.last().contains("permission"), onlySet.last());
        assertFalse(store.has(owner(onlyIssue)));
        assertTrue(environment.given.isEmpty());
        assertTrue(maps.all().isEmpty());
    }

    @Test
    void viewSaysWhetherTheSenderHasArms() {
        run(steve.proxy, "view");
        assertEquals("Steve has no arms.", steve.last());

        store(steve, Color.RED);
        run(steve.proxy, "view");
        assertEquals("Steve has arms on record.", steve.last());
    }

    @Test
    void viewCanNameAnotherPlayerAndTheConsoleMustNameOne() {
        store(alex, Color.RED);
        environment.online(alex);
        CommandSender console = console();

        run(steve.proxy, "view", "Alex");
        assertEquals("Alex has arms on record.", steve.last());

        run(console, "view");
        assertTrue(lastConsoleMessage.contains("console must name a player"), lastConsoleMessage);

        run(console, "view", "Alex");
        assertEquals("Alex has arms on record.", lastConsoleMessage);
    }

    @Test
    void viewRejectsTooManyArguments() {
        run(steve.proxy, "view", "Alex", "extra");

        assertTrue(steve.last().contains("Usage"), steve.last());
    }

    @Test
    void setStoresTheFileAsTheSendersArms() throws IOException {
        run(steve.proxy, "set", "york.png");

        assertEquals(Color.RED.getRGB(), store.load(owner(steve)).orElseThrow().getRGB(3, 3));
        assertEquals("Steve's arms are now york.png.", steve.last());
    }

    @Test
    void setCanNameAnotherPlayerAndLeavesTheSendersArmsAlone() throws IOException {
        environment.online(alex);

        run(steve.proxy, "set", "lancaster.png", "Alex");

        assertEquals(Color.BLUE.getRGB(), store.load(owner(alex)).orElseThrow().getRGB(3, 3));
        assertFalse(store.has(owner(steve)));
    }

    @Test
    void setFromTheConsoleMustNameAPlayer() {
        environment.online(alex);
        CommandSender console = console();

        run(console, "set", "york.png");
        assertTrue(lastConsoleMessage.contains("console must name a player"), lastConsoleMessage);
        assertFalse(store.has(owner(alex)));

        run(console, "set", "york.png", "Alex");
        assertTrue(store.has(owner(alex)));
    }

    @Test
    void setShowsTheImportersMessageAndKeepsExistingArms() throws IOException {
        store(steve, Color.RED);

        run(steve.proxy, "set", "absent.png");

        assertEquals("No file named absent.png in the import folder.", steve.last());
        assertEquals(Color.RED.getRGB(), store.load(owner(steve)).orElseThrow().getRGB(3, 3));
    }

    @Test
    void setNeedsAFileAndNoMoreThanAPlayer() {
        run(steve.proxy, "set");
        assertTrue(steve.last().contains("Usage"), steve.last());

        run(steve.proxy, "set", "york.png", "Alex", "extra");
        assertTrue(steve.last().contains("Usage"), steve.last());
    }

    @Test
    void setRedrawsTheMapsAlreadyIssuedToThatPlayerAndShowsThemToPlayers() {
        run(steve.proxy, "set", "york.png");
        run(steve.proxy, "issue");
        run(steve.proxy, "issue");
        FakeView first = environment.given.get(0).view;
        FakeView second = environment.given.get(1).view;
        assertEquals(RED_PIXEL, first.drawnCentrePixel());

        run(steve.proxy, "set", "lancaster.png");

        assertEquals(BLUE_PIXEL, first.drawnCentrePixel());
        assertEquals(BLUE_PIXEL, second.drawnCentrePixel());
        assertEquals(Arrays.asList(first.proxy, second.proxy), environment.shown);
        assertEquals("Steve's arms are now lancaster.png.", steve.last());
    }

    @Test
    void setLeavesTheMapsOfOtherPlayersAlone() {
        environment.online(alex);
        store(alex, Color.RED);
        run(steve.proxy, "issue", "Alex");
        FakeView alexsMap = environment.given.get(0).view;
        MapRenderer renderer = alexsMap.renderers.get(0);

        run(steve.proxy, "set", "lancaster.png");

        assertSame(renderer, alexsMap.renderers.get(0));
        assertTrue(environment.shown.isEmpty());
    }

    @Test
    void aFailedSetLeavesIssuedMapsAlone() {
        store(steve, Color.RED);
        run(steve.proxy, "issue");
        FakeView map = environment.given.get(0).view;
        MapRenderer renderer = map.renderers.get(0);

        run(steve.proxy, "set", "absent.png");

        assertSame(renderer, map.renderers.get(0));
        assertTrue(environment.shown.isEmpty());
    }

    @Test
    void setStillSucceedsWhenAnIssuedMapCannotBeRedrawnAndLogsIt() {
        store(steve, Color.RED);
        run(steve.proxy, "issue");
        environment.views.clear();

        run(steve.proxy, "set", "lancaster.png");

        assertEquals("Steve's arms are now lancaster.png.", steve.last());
        assertEquals(1, logged.size());
        assertTrue(logged.get(0).contains("no longer exists"), logged.get(0));
        assertTrue(environment.shown.isEmpty());
    }

    @Test
    void namingAnOfflinePlayerIsReported() {
        run(steve.proxy, "set", "york.png", "Alex");
        assertEquals("There is no online player named Alex.", steve.last());

        run(steve.proxy, "issue", "Alex");
        assertEquals("There is no online player named Alex.", steve.last());

        run(steve.proxy, "view", "Alex");
        assertEquals("There is no online player named Alex.", steve.last());
        assertFalse(store.has(owner(alex)));
    }

    @Test
    void issueGivesTheSenderAMapOfTheirArmsAndRecordsIt() {
        store(steve, Color.RED);

        run(steve.proxy, "issue");

        assertEquals(1, environment.given.size());
        Given given = environment.given.get(0);
        assertSame(steve, given.target);
        assertEquals(owner(steve), given.owner);
        assertEquals(1, given.view.renderers.size());
        assertEquals(Optional.of(owner(steve)), maps.ownerOf(given.view.id));
        assertEquals(Collections.singletonList(steve), environment.mapsCreatedFor);
        assertEquals("Gave Steve a map of their arms.", steve.last());
    }

    @Test
    void issueCanNameAPlayerWhoGetsTheirOwnArmsNotTheSenders() {
        store(steve, Color.RED);
        store(alex, Color.BLUE);
        environment.online(alex);

        run(steve.proxy, "issue", "Alex");

        Given given = environment.given.get(0);
        assertSame(alex, given.target);
        assertEquals(owner(alex), given.owner);
        assertEquals(Optional.of(owner(alex)), maps.ownerOf(given.view.id));
        assertEquals(Collections.singletonList(alex), environment.mapsCreatedFor);
    }

    @Test
    void issueFromTheConsoleMustNameAPlayer() {
        store(alex, Color.RED);
        environment.online(alex);
        CommandSender console = console();

        run(console, "issue");
        assertTrue(lastConsoleMessage.contains("console must name a player"), lastConsoleMessage);
        assertTrue(environment.given.isEmpty());

        run(console, "issue", "Alex");
        assertEquals(1, environment.given.size());
    }

    @Test
    void issueWithNoArmsCreatesAndGivesNothing() {
        run(steve.proxy, "issue");

        assertEquals("Steve has no arms to issue.", steve.last());
        assertTrue(environment.mapsCreatedFor.isEmpty());
        assertTrue(environment.given.isEmpty());
        assertTrue(maps.all().isEmpty());
    }

    @Test
    void issueWithUnreadableArmsIsLoggedAndGivesNothing() throws IOException {
        store(steve, Color.RED);
        try (Stream<Path> files = Files.walk(dataFolder.resolve("arms"))) {
            for (Path file : files.filter(Files::isRegularFile).collect(Collectors.toList())) {
                Files.write(file, "not a png".getBytes(StandardCharsets.UTF_8));
            }
        }

        run(steve.proxy, "issue");

        assertEquals("Could not issue those arms; see the server log.", steve.last());
        assertEquals(1, logged.size());
        assertTrue(logged.get(0).contains("Steve"), logged.get(0));
        assertTrue(environment.given.isEmpty());
    }

    @Test
    void issueRejectsTooManyArguments() {
        store(steve, Color.RED);

        run(steve.proxy, "issue", "Alex", "extra");

        assertTrue(steve.last().contains("Usage"), steve.last());
        assertTrue(environment.given.isEmpty());
    }

    @Test
    void theFirstArgumentCompletesToThePermittedSubcommands() {
        assertEquals(HeraldryCommand.SUBCOMMANDS, complete(steve.proxy, ""));
        assertEquals(Collections.singletonList("issue"), complete(steve.proxy, "I"));
        assertEquals(Collections.emptyList(), complete(alex.proxy, ""));

        FakePlayer onlyView = new FakePlayer("Vic", "e3d7c4f2-5c80-4f3b-9f20-4c9d0e1f2a3b", "heraldry.view");
        assertEquals(Collections.singletonList("view"), complete(onlyView.proxy, ""));
    }

    @Test
    void setCompletesTheImportFolderThenPlayerNames() {
        environment.online(alex);

        assertEquals(Arrays.asList("lancaster.png", "york.png"), complete(steve.proxy, "set", ""));
        assertEquals(Collections.singletonList("york.png"), complete(steve.proxy, "set", "y"));
        assertEquals(Collections.singletonList("Alex"), complete(steve.proxy, "set", "york.png", "a"));
        assertEquals(Collections.emptyList(), complete(steve.proxy, "set", "york.png", "Alex", ""));
    }

    @Test
    void viewAndIssueCompletePlayerNames() {
        environment.online(alex);

        assertEquals(Collections.singletonList("Alex"), complete(steve.proxy, "view", "al"));
        assertEquals(Collections.singletonList("Alex"), complete(steve.proxy, "issue", ""));
        assertEquals(Collections.emptyList(), complete(steve.proxy, "issue", "z"));
        assertEquals(Collections.emptyList(), complete(steve.proxy, "issue", "Alex", ""));
    }

    @Test
    void nothingIsCompletedForASubcommandTheSenderCannotUseOrThatDoesNotExist() {
        environment.online(alex);

        assertEquals(Collections.emptyList(), complete(alex.proxy, "set", ""));
        assertEquals(Collections.emptyList(), complete(steve.proxy, "burn", ""));
    }

    private void run(CommandSender sender, String... args) {
        assertTrue(command.onCommand(sender, null, "heraldry", args));
    }

    private List<String> complete(CommandSender sender, String... args) {
        return command.onTabComplete(sender, null, "heraldry", args);
    }

    private void store(FakePlayer player, Color colour) {
        try {
            store.save(owner(player), filled(colour));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static ArmsOwner owner(FakePlayer player) {
        return ArmsOwner.player(player.id);
    }

    private String lastConsoleMessage;

    /** A sender that is not a player and has every permission, as the console does. */
    private CommandSender console() {
        return (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class}, (self, method, args) -> {
                    switch (method.getName()) {
                        case "hasPermission":
                            return true;
                        case "sendMessage":
                            lastConsoleMessage = (String) args[0];
                            return null;
                        case "hashCode":
                            return System.identityHashCode(self);
                        case "equals":
                            return self == args[0];
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private static void writePng(Path file, Color colour) throws IOException {
        assertTrue(ImageIO.write(filled(colour), "png", file.toFile()));
    }

    private static BufferedImage filled(Color colour) {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                image.setRGB(x, y, colour.getRGB());
            }
        }
        return image;
    }

    /** A player: {@link #proxy} is the sender of their commands, and this is what commands act on. */
    private final class FakePlayer implements HeraldryCommand.Bearer {
        final String name;
        final UUID id;
        final Set<String> permissions;
        final List<String> messages = new ArrayList<>();
        final CommandSender proxy;

        FakePlayer(String name, String id, String... permissions) {
            this.name = name;
            this.id = UUID.fromString(id);
            this.permissions = new HashSet<>(Arrays.asList(permissions));
            proxy = (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(),
                    new Class<?>[]{CommandSender.class}, (self, method, args) -> {
                        switch (method.getName()) {
                            case "hasPermission":
                                return this.permissions.contains(args[0]);
                            case "sendMessage":
                                messages.add((String) args[0]);
                                return null;
                            case "hashCode":
                                return System.identityHashCode(self);
                            case "equals":
                                return self == args[0];
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
            environment.senders.put(proxy, this);
        }

        String last() {
            return messages.get(messages.size() - 1);
        }

        @Override
        public UUID getUniqueId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public MapView createMap() {
            environment.mapsCreatedFor.add(this);
            FakeView view = new FakeView(environment.mapsCreatedFor.size());
            environment.views.put(view.proxy, view);
            return view.proxy;
        }

        @Override
        public void give(MapView view, ArmsOwner owner) {
            environment.given.add(new Given(this, environment.views.get(view), owner));
        }
    }

    /** Keeps only the id and renderer list of a {@link MapView}; no other method is expected. */
    private static final class FakeView {
        final int id;
        final List<MapRenderer> renderers = new ArrayList<>();
        final MapView proxy;

        FakeView(int id) {
            this.id = id;
            proxy = (MapView) Proxy.newProxyInstance(MapView.class.getClassLoader(), new Class<?>[]{MapView.class},
                    (self, method, args) -> {
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
            int centre = MapRasteriser.MAP_SIZE / 2;
            byte[] drawn = new byte[1];
            MapCanvas canvas = (MapCanvas) Proxy.newProxyInstance(MapCanvas.class.getClassLoader(),
                    new Class<?>[]{MapCanvas.class}, (canvasProxy, method, args) -> {
                        if ((Integer) args[0] == centre && (Integer) args[1] == centre) {
                            drawn[0] = (Byte) args[2];
                        }
                        return null;
                    });
            renderers.get(0).render(proxy, canvas, null);
            return drawn[0];
        }
    }

    private static final class Given {
        final FakePlayer target;
        final FakeView view;
        final ArmsOwner owner;

        Given(FakePlayer target, FakeView view, ArmsOwner owner) {
            this.target = target;
            this.view = view;
            this.owner = owner;
        }
    }

    private static final class FakeEnvironment implements HeraldryCommand.Environment {
        final Map<CommandSender, FakePlayer> senders = new HashMap<>();
        final Map<String, FakePlayer> players = new HashMap<>();
        final Map<MapView, FakeView> views = new HashMap<>();
        final List<FakePlayer> mapsCreatedFor = new ArrayList<>();
        final List<Given> given = new ArrayList<>();
        final List<MapView> shown = new ArrayList<>();

        MapView findMap(int mapId) {
            for (FakeView view : views.values()) {
                if (view.id == mapId) {
                    return view.proxy;
                }
            }
            return null;
        }

        void online(FakePlayer player) {
            players.put(player.name, player);
        }

        @Override
        public HeraldryCommand.Bearer senderAsPlayer(CommandSender sender) {
            return senders.get(sender);
        }

        @Override
        public HeraldryCommand.Bearer findPlayer(String name) {
            return players.get(name);
        }

        @Override
        public List<String> playerNames() {
            return new ArrayList<>(players.keySet());
        }

        @Override
        public void showToPlayers(MapView view) {
            shown.add(view);
        }
    }
}
