package com.dansplugins.heraldry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssuedMapsTest {

    private static final ArmsOwner PLAYER = ArmsOwner.player(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"));
    private static final ArmsOwner FACTION = ArmsOwner.faction("house_of_york");

    @TempDir
    Path root;

    @Test
    void missingFileMeansNoMapsHaveBeenIssued() throws IOException {
        IssuedMaps maps = IssuedMaps.load(root.resolve("maps.txt"));

        assertEquals(Optional.empty(), maps.ownerOf(7));
        assertTrue(maps.all().isEmpty());
    }

    @Test
    void recordedMapsSurviveALoadOfTheSameFile() throws IOException {
        Path file = root.resolve("maps.txt");
        IssuedMaps maps = IssuedMaps.load(file);
        maps.record(7, PLAYER);
        maps.record(3, FACTION);

        IssuedMaps reloaded = IssuedMaps.load(file);

        assertEquals(Optional.of(PLAYER), reloaded.ownerOf(7));
        assertEquals(Optional.of(FACTION), reloaded.ownerOf(3));
        assertEquals(Optional.empty(), reloaded.ownerOf(5));
    }

    @Test
    void fileHoldsOneSortedLinePerMap() throws IOException {
        Path file = root.resolve("maps.txt");
        IssuedMaps maps = IssuedMaps.load(file);
        maps.record(12, PLAYER);
        maps.record(3, FACTION);

        assertEquals(Arrays.asList("3 FACTION:house_of_york", "12 PLAYER:0f8fad5b-d9cb-469f-a165-70867728950e"),
                Files.readAllLines(file, StandardCharsets.UTF_8));
        assertEquals(Arrays.asList(3, 12), List.copyOf(maps.all().keySet()));
    }

    @Test
    void recordingAgainReplacesTheMapsOwner() throws IOException {
        Path file = root.resolve("maps.txt");
        IssuedMaps maps = IssuedMaps.load(file);
        maps.record(7, PLAYER);
        maps.record(7, FACTION);

        assertEquals(Optional.of(FACTION), IssuedMaps.load(file).ownerOf(7));
        assertFalse(Files.exists(root.resolve("maps.txt.tmp")));
    }

    @Test
    void forgetStopsTrackingAMapAndReportsWhetherItWasTracked() throws IOException {
        Path file = root.resolve("maps.txt");
        IssuedMaps maps = IssuedMaps.load(file);
        maps.record(7, PLAYER);

        assertTrue(maps.forget(7));
        assertFalse(maps.forget(7));
        assertEquals(Optional.empty(), IssuedMaps.load(file).ownerOf(7));
    }

    @Test
    void missingParentDirectoriesAreCreated() throws IOException {
        Path file = root.resolve("data").resolve("maps.txt");

        IssuedMaps.load(file).record(1, FACTION);

        assertTrue(Files.isRegularFile(file));
    }

    @Test
    void blankLinesAreIgnored() throws IOException {
        Path file = root.resolve("maps.txt");
        Files.write(file, Arrays.asList("", "4 FACTION:house_of_york", "  "), StandardCharsets.UTF_8);

        assertEquals(Optional.of(FACTION), IssuedMaps.load(file).ownerOf(4));
    }

    @Test
    void malformedLinesAreReportedWithTheirLineNumber() throws IOException {
        assertLoadFails("line 2", "4 FACTION:house_of_york", "4FACTION:house_of_york");
        assertLoadFails("line 1", "four FACTION:house_of_york");
        assertLoadFails("line 1", "4 KINGDOM:house_of_york");
        assertLoadFails("line 1", "4 PLAYER:not-a-uuid");
        assertLoadFails("line 1", "4 FACTION:house of york");
    }

    @Test
    void repeatedMapIdsAreReported() throws IOException {
        assertLoadFails("repeats map id 4", "4 FACTION:house_of_york", "4 FACTION:house_of_lancaster");
    }

    @Test
    void failedWriteLeavesTheRecordUnchanged() throws IOException {
        Path file = root.resolve("maps.txt");
        IssuedMaps maps = IssuedMaps.load(file);
        maps.record(7, PLAYER);
        // A directory where the temporary file would go makes the next write fail.
        Files.createDirectory(root.resolve("maps.txt.tmp"));

        assertThrows(IOException.class, () -> maps.record(7, FACTION));
        assertThrows(IOException.class, () -> maps.record(8, FACTION));
        assertThrows(IOException.class, () -> maps.forget(7));

        assertEquals(Optional.of(PLAYER), maps.ownerOf(7));
        assertEquals(Optional.empty(), maps.ownerOf(8));
    }

    @Test
    void nullArgumentsAreRejected() throws IOException {
        IssuedMaps maps = IssuedMaps.load(root.resolve("maps.txt"));

        assertThrows(IllegalArgumentException.class, () -> IssuedMaps.load(null));
        assertThrows(IllegalArgumentException.class, () -> maps.record(1, null));
    }

    private void assertLoadFails(String expectedMessagePart, String... lines) throws IOException {
        Path file = root.resolve("bad.txt");
        Files.write(file, Arrays.asList(lines), StandardCharsets.UTF_8);

        IOException e = assertThrows(IOException.class, () -> IssuedMaps.load(file));
        assertTrue(e.getMessage().contains(expectedMessagePart), e.getMessage());
    }
}
