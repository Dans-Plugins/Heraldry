package com.dansplugins.heraldry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmsStoreTest {

    private static final ArmsOwner PLAYER = ArmsOwner.player(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"));
    private static final ArmsOwner FACTION = ArmsOwner.faction("0f8fad5b-d9cb-469f-a165-70867728950e");

    @TempDir
    Path root;

    @Test
    void ownerWithNoArmsLoadsEmpty() throws IOException {
        ArmsStore store = new ArmsStore(root);

        assertEquals(Optional.empty(), store.load(PLAYER));
        assertFalse(store.has(PLAYER));
    }

    @Test
    void savedArmsSurviveANewStoreOnTheSameDirectory() throws IOException {
        new ArmsStore(root).save(PLAYER, halved(Color.RED, Color.WHITE));

        BufferedImage loaded = new ArmsStore(root).load(PLAYER).orElseThrow();

        assertEquals(20, loaded.getWidth());
        assertEquals(10, loaded.getHeight());
        assertEquals(Color.RED.getRGB(), loaded.getRGB(2, 5));
        assertEquals(Color.WHITE.getRGB(), loaded.getRGB(17, 5));
    }

    @Test
    void savingAgainReplacesTheOwnersArms() throws IOException {
        ArmsStore store = new ArmsStore(root);
        store.save(PLAYER, halved(Color.RED, Color.RED));
        store.save(PLAYER, halved(Color.BLUE, Color.BLUE));

        assertEquals(Color.BLUE.getRGB(), store.load(PLAYER).orElseThrow().getRGB(2, 5));
        assertFalse(Files.exists(store.fileFor(PLAYER).resolveSibling(store.fileFor(PLAYER).getFileName() + ".tmp")));
    }

    @Test
    void playerAndFactionWithTheSameIdKeepSeparateArms() throws IOException {
        ArmsStore store = new ArmsStore(root);
        store.save(PLAYER, halved(Color.RED, Color.RED));
        store.save(FACTION, halved(Color.BLUE, Color.BLUE));

        assertEquals(Color.RED.getRGB(), store.load(PLAYER).orElseThrow().getRGB(2, 5));
        assertEquals(Color.BLUE.getRGB(), store.load(FACTION).orElseThrow().getRGB(2, 5));
    }

    @Test
    void filesAreLaidOutByOwnerTypeAndId() throws IOException {
        ArmsStore store = new ArmsStore(root);
        store.save(FACTION, halved(Color.RED, Color.RED));

        assertTrue(Files.isRegularFile(root.resolve("faction").resolve("0f8fad5b-d9cb-469f-a165-70867728950e.png")));
    }

    @Test
    void deleteRemovesTheArmsAndReportsWhetherThereWereAny() throws IOException {
        ArmsStore store = new ArmsStore(root);
        store.save(PLAYER, halved(Color.RED, Color.RED));

        assertTrue(store.delete(PLAYER));
        assertFalse(store.has(PLAYER));
        assertFalse(store.delete(PLAYER));
    }

    @Test
    void unreadableStoredFileIsReportedRatherThanTreatedAsNoArms() throws IOException {
        ArmsStore store = new ArmsStore(root);
        Path file = store.fileFor(PLAYER);
        Files.createDirectories(file.getParent());
        Files.write(file, "not a png".getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> store.load(PLAYER));
    }

    @Test
    void nullArgumentsAreRejected() {
        ArmsStore store = new ArmsStore(root);

        assertThrows(IllegalArgumentException.class, () -> new ArmsStore(null));
        assertThrows(IllegalArgumentException.class, () -> store.save(PLAYER, null));
        assertThrows(IllegalArgumentException.class, () -> store.load(null));
    }

    private static BufferedImage halved(Color left, Color right) {
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 20; x++) {
                image.setRGB(x, y, (x < 10 ? left : right).getRGB());
            }
        }
        return image;
    }
}
