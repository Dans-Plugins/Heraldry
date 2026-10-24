package com.dansplugins.heraldry;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArmsOwnerTest {

    private static final UUID PLAYER_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

    @Test
    void playerOwnerIsKeyedByUuid() {
        ArmsOwner owner = ArmsOwner.player(PLAYER_ID);

        assertEquals(ArmsOwner.Type.PLAYER, owner.getType());
        assertEquals("0f8fad5b-d9cb-469f-a165-70867728950e", owner.getId());
    }

    @Test
    void factionOwnerIsKeyedByFactionId() {
        ArmsOwner owner = ArmsOwner.faction("house_of_york-1");

        assertEquals(ArmsOwner.Type.FACTION, owner.getType());
        assertEquals("house_of_york-1", owner.getId());
    }

    @Test
    void ownersWithTheSameTypeAndIdAreEqual() {
        assertEquals(ArmsOwner.player(PLAYER_ID), ArmsOwner.player(PLAYER_ID));
        assertEquals(ArmsOwner.player(PLAYER_ID).hashCode(), ArmsOwner.player(PLAYER_ID).hashCode());
    }

    @Test
    void playerAndFactionWithTheSameIdAreDifferentOwners() {
        assertNotEquals(ArmsOwner.player(PLAYER_ID), ArmsOwner.faction(PLAYER_ID.toString()));
    }

    @Test
    void idsThatCouldEscapeTheStoreAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction("../player/x"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction("a/b"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction("a.b"));
    }

    @Test
    void emptyNullAndOverlongIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction(""));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction(null));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction("a".repeat(65)));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.player(null));
    }
}
