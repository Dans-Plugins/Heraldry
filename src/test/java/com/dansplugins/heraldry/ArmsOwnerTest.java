package com.dansplugins.heraldry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void toStringNamesTheTypeAndId() {
        assertEquals("FACTION:house_of_york-1", ArmsOwner.faction("house_of_york-1").toString());
    }

    @Test
    void parseReadsBackWhatToStringWrites() {
        ArmsOwner player = ArmsOwner.player(PLAYER_ID);
        ArmsOwner faction = ArmsOwner.faction("house_of_york-1");

        assertEquals(player, ArmsOwner.parse(player.toString()));
        assertEquals(faction, ArmsOwner.parse(faction.toString()));
    }

    @Test
    void parseRejectsTextThatIsNotAnOwner() {
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse(null));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("house_of_york"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("KINGDOM:house_of_york"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("faction:house_of_york"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("FACTION:../player/x"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("PLAYER:house_of_york"));
    }

    @Test
    void parseRejectsAbbreviatedPlayerUuids() {
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("PLAYER:1-1-1-1-1"));
        assertThrows(IllegalArgumentException.class,
                () -> ArmsOwner.parse("PLAYER:0F8FAD5B-D9CB-469F-A165-70867728950E"));
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

    @Test
    void idsOfOneAndOfSixtyFourCharactersAreAccepted() {
        assertEquals("a", ArmsOwner.faction("a").getId());
        assertEquals("a".repeat(64), ArmsOwner.faction("a".repeat(64)).getId());
    }

    @Test
    void idsMayUseLettersOfEitherCaseDigitsHyphensAndUnderscores() {
        assertEquals("House_Of-York_1485", ArmsOwner.faction("House_Of-York_1485").getId());
    }

    @Test
    void theWholeIdMustBeSafeNotJustPartOfIt() {
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction("york\n"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction(" york"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction("york lancaster"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction("york:lancaster"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.faction("yörk"));
    }

    @Test
    void factionIdsAreCaseSensitive() {
        assertNotEquals(ArmsOwner.faction("York"), ArmsOwner.faction("york"));
    }

    @Test
    void anOwnerIsNotEqualToNullOrToAnotherKindOfObject() {
        ArmsOwner owner = ArmsOwner.faction("york");

        assertFalse(owner.equals(null));
        assertFalse(owner.equals("FACTION:york"));
        assertTrue(owner.equals(owner));
    }

    @Test
    void aPlayerOwnerIsWrittenAsPlayerThenTheCanonicalUuid() {
        assertEquals("PLAYER:0f8fad5b-d9cb-469f-a165-70867728950e", ArmsOwner.player(PLAYER_ID).toString());
    }

    @Test
    void parseRejectsAColonInTheId() {
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("FACTION:york:lancaster"));
        assertThrows(IllegalArgumentException.class,
                () -> ArmsOwner.parse("PLAYER:" + PLAYER_ID + ":extra"));
    }

    @Test
    void parseRejectsAnEmptyTypeOrAnEmptyId() {
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse(":york"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("FACTION:"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("PLAYER:"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse(""));
    }

    @Test
    void parseDoesNotTrimWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse(" FACTION:york"));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("FACTION:york "));
        assertThrows(IllegalArgumentException.class, () -> ArmsOwner.parse("PLAYER: " + PLAYER_ID));
    }

    @Test
    void parseErrorsNameTheTextThatWasRejected() {
        assertMessageContains("KINGDOM", () -> ArmsOwner.parse("KINGDOM:york"));
        assertMessageContains("house_of_york", () -> ArmsOwner.parse("house_of_york"));
        assertMessageContains("not-a-uuid", () -> ArmsOwner.parse("PLAYER:not-a-uuid"));
        assertMessageContains("1-1-1-1-1", () -> ArmsOwner.parse("PLAYER:1-1-1-1-1"));
        assertMessageContains("a/b", () -> ArmsOwner.parse("FACTION:a/b"));
    }

    @Test
    void anUnparseablePlayerUuidKeepsTheUnderlyingCause() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ArmsOwner.parse("PLAYER:not-a-uuid"));

        assertNotNull(e.getCause());
    }

    private static void assertMessageContains(String expected, Executable parse) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, parse);
        assertTrue(e.getMessage().contains(expected),
                () -> "Expected the message to contain '" + expected + "' but was: " + e.getMessage());
    }
}
