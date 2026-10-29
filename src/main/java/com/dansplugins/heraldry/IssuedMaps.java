package com.dansplugins.heraldry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Remembers which owner each issued map belongs to, so its renderer can be added back after a restart.
 *
 * <p>The server forgets map renderers when it stops, but keeps the map views themselves. Only the
 * owner is recorded against a map id, never the arms: the arms are looked up from the
 * {@link ArmsStore} when the map is drawn, so an owner who changes their arms changes every map
 * already issued.
 *
 * <p>The file holds one {@code <map id> <owner>} line per map, sorted by map id, for example
 * {@code 42 FACTION:house_of_york}.
 */
public final class IssuedMaps {

    private final Path file;
    private final Map<Integer, ArmsOwner> owners;

    private IssuedMaps(Path file, Map<Integer, ArmsOwner> owners) {
        this.file = file;
        this.owners = owners;
    }

    /**
     * Reads the issued maps recorded in {@code file}. A missing file means no maps have been issued.
     *
     * @throws IOException if the file cannot be read or holds a line that is not a valid entry
     */
    public static IssuedMaps load(Path file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        Map<Integer, ArmsOwner> owners = new TreeMap<>();
        if (Files.isRegularFile(file)) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                parseLine(file, i + 1, line, owners);
            }
        }
        return new IssuedMaps(file, owners);
    }

    /** Records that the map with this id bears the owner's arms, replacing any earlier owner. */
    public void record(int mapId, ArmsOwner owner) throws IOException {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        ArmsOwner previous = owners.put(mapId, owner);
        try {
            save();
        } catch (IOException e) {
            restore(mapId, previous);
            throw e;
        }
    }

    /** Stops tracking the map with this id. Returns whether it was tracked. */
    public boolean forget(int mapId) throws IOException {
        ArmsOwner previous = owners.remove(mapId);
        if (previous == null) {
            return false;
        }
        try {
            save();
        } catch (IOException e) {
            owners.put(mapId, previous);
            throw e;
        }
        return true;
    }

    /** Returns the owner of the map with this id, or empty if the map was not issued by Heraldry. */
    public Optional<ArmsOwner> ownerOf(int mapId) {
        return Optional.ofNullable(owners.get(mapId));
    }

    /** Every tracked map id and its owner, in ascending map id order. */
    public Map<Integer, ArmsOwner> all() {
        return Collections.unmodifiableMap(owners);
    }

    private static void parseLine(Path file, int lineNumber, String line, Map<Integer, ArmsOwner> owners)
            throws IOException {
        String[] parts = line.split(" ", -1);
        if (parts.length != 2) {
            throw new IOException(file + " line " + lineNumber + " must be '<map id> <owner>', but was: " + line);
        }
        int mapId;
        ArmsOwner owner;
        try {
            mapId = Integer.parseInt(parts[0]);
            owner = ArmsOwner.parse(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new IOException(file + " line " + lineNumber + " is not a valid issued map: " + e.getMessage(), e);
        }
        if (owners.put(mapId, owner) != null) {
            throw new IOException(file + " line " + lineNumber + " repeats map id " + mapId);
        }
    }

    private void restore(int mapId, ArmsOwner previous) {
        if (previous == null) {
            owners.remove(mapId);
        } else {
            owners.put(mapId, previous);
        }
    }

    /** Written to a temporary name and moved into place, so a crash mid-write keeps the previous file. */
    private void save() throws IOException {
        List<String> lines = new ArrayList<>(owners.size());
        for (Map.Entry<Integer, ArmsOwner> entry : owners.entrySet()) {
            lines.add(entry.getKey() + " " + entry.getValue());
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
