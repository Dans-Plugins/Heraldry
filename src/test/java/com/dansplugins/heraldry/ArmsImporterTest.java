package com.dansplugins.heraldry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmsImporterTest {

    private static final ArmsOwner PLAYER = ArmsOwner.player(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"));

    @TempDir
    Path dataFolder;

    @Test
    void importedPngBecomesTheOwnersStoredArms() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        writePng(imports.resolve("york.png"), Color.RED);
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));

        BufferedImage imported = new ArmsImporter(imports, store).importArms(PLAYER, "york.png");

        assertEquals(Color.RED.getRGB(), imported.getRGB(3, 3));
        assertEquals(Color.RED.getRGB(), store.load(PLAYER).orElseThrow().getRGB(3, 3));
    }

    @Test
    void importingAgainReplacesTheOwnersArms() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        writePng(imports.resolve("york.png"), Color.RED);
        writePng(imports.resolve("lancaster.png"), Color.BLUE);
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));
        ArmsImporter importer = new ArmsImporter(imports, store);

        importer.importArms(PLAYER, "york.png");
        importer.importArms(PLAYER, "lancaster.png");

        assertEquals(Color.BLUE.getRGB(), store.load(PLAYER).orElseThrow().getRGB(3, 3));
    }

    @Test
    void missingFileIsReportedAndLeavesExistingArmsAlone() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));
        store.save(PLAYER, filled(Color.RED));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new ArmsImporter(imports, store).importArms(PLAYER, "absent.png"));

        assertTrue(e.getMessage().contains("absent.png"), e.getMessage());
        assertEquals(Color.RED.getRGB(), store.load(PLAYER).orElseThrow().getRGB(3, 3));
    }

    @Test
    void unreadableImageIsReportedAndNothingIsStored() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        Files.write(imports.resolve("broken.png"), "not a png".getBytes(StandardCharsets.UTF_8));
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new ArmsImporter(imports, store).importArms(PLAYER, "broken.png"));

        assertTrue(e.getMessage().contains("not a readable PNG"), e.getMessage());
        assertFalse(store.has(PLAYER));
    }

    @Test
    void pathsOutsideTheImportDirectoryAreRejected() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        writePng(dataFolder.resolve("outside.png"), Color.RED);
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));
        ArmsImporter importer = new ArmsImporter(imports, store);

        for (String name : Arrays.asList("../outside.png", "..\\outside.png", "sub/outside.png",
                dataFolder.resolve("outside.png").toString(), "..png", ".png", "")) {
            assertThrows(IllegalArgumentException.class, () -> importer.importArms(PLAYER, name), name);
        }
        assertFalse(store.has(PLAYER));
    }

    @Test
    void svgIsRejectedWithAMessageAskingForAPng() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        Files.write(imports.resolve("arms.svg"), "<svg/>".getBytes(StandardCharsets.UTF_8));
        ArmsImporter importer = new ArmsImporter(imports, new ArmsStore(dataFolder.resolve("arms")));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> importer.importArms(PLAYER, "arms.svg"));

        assertTrue(e.getMessage().contains("PNG"), e.getMessage());
    }

    @Test
    void imagesOverTheSizeLimitAreRejectedInEitherDimensionAndLeaveExistingArmsAlone() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        writePng(imports.resolve("york.png"), Color.RED);
        int tooBig = ArmsImporter.MAX_DIMENSION + 1;
        writePng(imports.resolve("wide.png"), tooBig, 1);
        writePng(imports.resolve("tall.png"), 1, tooBig);
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));
        ArmsImporter importer = new ArmsImporter(imports, store);
        importer.importArms(PLAYER, "york.png");

        IllegalArgumentException wide = assertThrows(IllegalArgumentException.class,
                () -> importer.importArms(PLAYER, "wide.png"));
        IllegalArgumentException tall = assertThrows(IllegalArgumentException.class,
                () -> importer.importArms(PLAYER, "tall.png"));

        assertTrue(wide.getMessage().contains("wide.png is 2049x1 pixels"), wide.getMessage());
        assertTrue(tall.getMessage().contains("tall.png is 1x2049 pixels"), tall.getMessage());
        assertEquals(Color.RED.getRGB(), store.load(PLAYER).get().getRGB(3, 3));
    }

    @Test
    void imagesAtTheSizeLimitAreImported() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        writePng(imports.resolve("banner.png"), ArmsImporter.MAX_DIMENSION, 1);
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));

        BufferedImage imported = new ArmsImporter(imports, store).importArms(PLAYER, "banner.png");

        assertEquals(ArmsImporter.MAX_DIMENSION, imported.getWidth());
        assertTrue(store.has(PLAYER));
    }

    @Test
    void listReturnsTheImportablePngsSorted() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        writePng(imports.resolve("york.png"), Color.RED);
        writePng(imports.resolve("lancaster.png"), Color.BLUE);
        Files.write(imports.resolve("notes.txt"), "x".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(imports.resolve("folder.png"));

        ArmsImporter importer = new ArmsImporter(imports, new ArmsStore(dataFolder.resolve("arms")));

        assertEquals(Arrays.asList("lancaster.png", "york.png"), importer.list());
    }

    @Test
    void missingImportDirectoryListsNothing() throws IOException {
        ArmsImporter importer = new ArmsImporter(dataFolder.resolve("import"), new ArmsStore(dataFolder.resolve("arms")));

        assertEquals(Collections.emptyList(), importer.list());
    }

    @Test
    void aNameOfSixtyFourCharactersBeforeTheExtensionIsTheLongestAccepted() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        String longest = "a".repeat(64) + ".png";
        String tooLong = "a".repeat(65) + ".png";
        writePng(imports.resolve(longest), Color.RED);
        writePng(imports.resolve(tooLong), Color.BLUE);
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));
        ArmsImporter importer = new ArmsImporter(imports, store);

        importer.importArms(PLAYER, longest);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> importer.importArms(PLAYER, tooLong));

        assertTrue(e.getMessage().contains("plain .png name"), e.getMessage());
        assertTrue(e.getMessage().endsWith(tooLong), e.getMessage());
        assertEquals(Color.RED.getRGB(), store.load(PLAYER).orElseThrow().getRGB(3, 3));
    }

    @Test
    void theExtensionMustBeLowerCasePngButSvgIsRecognisedInAnyCase() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        writePng(imports.resolve("arms.PNG"), Color.RED);
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));
        ArmsImporter importer = new ArmsImporter(imports, store);

        IllegalArgumentException upperPng = assertThrows(IllegalArgumentException.class,
                () -> importer.importArms(PLAYER, "arms.PNG"));
        IllegalArgumentException upperSvg = assertThrows(IllegalArgumentException.class,
                () -> importer.importArms(PLAYER, "arms.SVG"));

        assertTrue(upperPng.getMessage().contains("plain .png name"), upperPng.getMessage());
        assertTrue(upperSvg.getMessage().startsWith("SVG arms are not supported"), upperSvg.getMessage());
        assertFalse(store.has(PLAYER));
    }

    @Test
    void namesMayContainDotsHyphensAndUnderscoresButMayOnlyStartWithALetterDigitOrUnderscore()
            throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        for (String name : Arrays.asList("_york.png", "9th-legion.png", "house_of-york.v2.png",
                "-york.png", ".york.png", "house of york.png", "york+.png")) {
            writePng(imports.resolve(name), Color.RED);
        }
        ArmsImporter importer = new ArmsImporter(imports, new ArmsStore(dataFolder.resolve("arms")));

        for (String accepted : Arrays.asList("_york.png", "9th-legion.png", "house_of-york.v2.png")) {
            assertEquals(Color.RED.getRGB(), importer.importArms(PLAYER, accepted).getRGB(3, 3), accepted);
        }
        for (String rejected : Arrays.asList("-york.png", ".york.png", "house of york.png", "york+.png")) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> importer.importArms(PLAYER, rejected), rejected);
            assertTrue(e.getMessage().contains("plain .png name"), rejected + ": " + e.getMessage());
        }
    }

    @Test
    void listLeavesOutFilesWhoseNamesCannotBeImported() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        for (String name : Arrays.asList("york.png", "arms.PNG", "-york.png", ".york.png",
                "house of york.png", "a".repeat(65) + ".png", "a".repeat(64) + ".png")) {
            writePng(imports.resolve(name), Color.RED);
        }
        ArmsImporter importer = new ArmsImporter(imports, new ArmsStore(dataFolder.resolve("arms")));

        assertEquals(Arrays.asList("a".repeat(64) + ".png", "york.png"), importer.list());
    }

    @Test
    void aFolderWithAValidNameIsReportedAsMissing() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        Files.createDirectories(imports.resolve("folder.png"));
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new ArmsImporter(imports, store).importArms(PLAYER, "folder.png"));

        assertEquals("No file named folder.png in the import folder.", e.getMessage());
        assertFalse(store.has(PLAYER));
    }

    @Test
    void theStoredArmsOutliveTheImportFile() throws IOException {
        Path imports = Files.createDirectories(dataFolder.resolve("import"));
        Path file = imports.resolve("york.png");
        writePng(file, Color.RED);
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));

        new ArmsImporter(imports, store).importArms(PLAYER, "york.png");
        Files.delete(file);

        assertEquals(Color.RED.getRGB(), store.load(PLAYER).orElseThrow().getRGB(3, 3));
    }

    @Test
    void nullArgumentsAreRejected() {
        ArmsStore store = new ArmsStore(dataFolder.resolve("arms"));
        ArmsImporter importer = new ArmsImporter(dataFolder, store);

        assertThrows(IllegalArgumentException.class, () -> new ArmsImporter(null, store));
        assertThrows(IllegalArgumentException.class, () -> new ArmsImporter(dataFolder, null));
        assertThrows(IllegalArgumentException.class, () -> importer.importArms(null, "york.png"));
        assertThrows(IllegalArgumentException.class, () -> importer.importArms(PLAYER, null));
    }

    private static void writePng(Path file, Color colour) throws IOException {
        assertTrue(ImageIO.write(filled(colour), "png", file.toFile()));
    }

    private static void writePng(Path file, int width, int height) throws IOException {
        assertTrue(ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", file.toFile()));
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
}
