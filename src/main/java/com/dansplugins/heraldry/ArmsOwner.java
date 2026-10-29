package com.dansplugins.heraldry;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Who a coat of arms belongs to: a player, or a Medieval Factions faction.
 *
 * <p>Arms are keyed to their owner rather than to a map item, so a lost map can be reissued and a
 * stolen one is not authoritative.
 */
public final class ArmsOwner {

    /** The kind of owner. */
    public enum Type {
        PLAYER,
        FACTION
    }

    /** Ids are used as file names, so only characters that are safe in a path are allowed. */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final Type type;
    private final String id;

    private ArmsOwner(Type type, String id) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (id == null || !VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "Owner id must be 1-64 letters, digits, '-' or '_', but was: " + id);
        }
        this.type = type;
        this.id = id;
    }

    public static ArmsOwner player(UUID playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("playerId must not be null");
        }
        return new ArmsOwner(Type.PLAYER, playerId.toString());
    }

    public static ArmsOwner faction(String factionId) {
        return new ArmsOwner(Type.FACTION, factionId);
    }

    /**
     * Reads an owner back from its {@link #toString()} form, for example {@code FACTION:house_of_york}.
     *
     * @throws IllegalArgumentException if the text is not a valid owner
     */
    public static ArmsOwner parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Owner text must not be null");
        }
        int colon = text.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Owner must be written as TYPE:id, but was: " + text);
        }
        String typeName = text.substring(0, colon);
        String id = text.substring(colon + 1);
        if (Type.PLAYER.name().equals(typeName)) {
            ArmsOwner owner;
            try {
                owner = player(UUID.fromString(id));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Player owner id must be a UUID, but was: " + id, e);
            }
            // UUID.fromString accepts abbreviated forms; only the canonical one names the stored file.
            if (!owner.getId().equals(id)) {
                throw new IllegalArgumentException("Player owner id must be a canonical UUID, but was: " + id);
            }
            return owner;
        }
        if (Type.FACTION.name().equals(typeName)) {
            return faction(id);
        }
        throw new IllegalArgumentException("Owner type must be PLAYER or FACTION, but was: " + typeName);
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ArmsOwner)) {
            return false;
        }
        ArmsOwner that = (ArmsOwner) other;
        return type == that.type && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }

    @Override
    public String toString() {
        return type + ":" + id;
    }
}
