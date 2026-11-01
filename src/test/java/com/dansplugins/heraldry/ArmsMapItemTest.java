package com.dansplugins.heraldry;

import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmsMapItemTest {

    private static final ArmsOwner PLAYER = ArmsOwner.player(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"));
    private static final ArmsOwner FACTION = ArmsOwner.faction("house_of_york");

    @Test
    void theDisplayNameNamesTheOwner() {
        assertEquals("Arms of House of York", ArmsMapItem.displayName("House of York"));
        assertEquals("Arms of Steve", ArmsMapItem.displayName("  Steve "));
    }

    @Test
    void theLoreSaysWhatKindOfOwnerAndEndsWithTheTag() {
        assertEquals(Arrays.asList("Arms of a player", ArmsMapItem.LORE_TAG), ArmsMapItem.lore(PLAYER));
        assertEquals(Arrays.asList("Arms of a faction", ArmsMapItem.LORE_TAG), ArmsMapItem.lore(FACTION));
    }

    @Test
    void labellingSetsTheViewNameAndLore() {
        FakeMeta meta = new FakeMeta();
        MapView view = fakeView();

        ArmsMapItem.label(meta, view, FACTION, "House of York");

        assertSame(view, meta.calls.get("setMapView"));
        assertEquals("Arms of House of York", meta.calls.get("setDisplayName"));
        assertEquals(Arrays.asList("Arms of a faction", ArmsMapItem.LORE_TAG), meta.calls.get("setLore"));
    }

    @Test
    void invalidArgumentsLeaveTheMetaUntouched() {
        FakeMeta meta = new FakeMeta();
        MapView view = fakeView();

        assertThrows(IllegalArgumentException.class, () -> ArmsMapItem.label(meta, view, PLAYER, " "));
        assertThrows(IllegalArgumentException.class, () -> ArmsMapItem.label(meta, view, PLAYER, null));
        assertThrows(IllegalArgumentException.class, () -> ArmsMapItem.label(meta, view, null, "Steve"));
        assertThrows(IllegalArgumentException.class, () -> ArmsMapItem.label(meta, null, PLAYER, "Steve"));
        assertThrows(IllegalArgumentException.class,
                () -> ArmsMapItem.label((ArmsMapItem.Target) null, view, PLAYER, "Steve"));

        assertTrue(meta.calls.isEmpty());
    }

    @Test
    void aNullMapMetaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ArmsMapItem.label((MapMeta) null, fakeView(), PLAYER, "Steve"));
    }

    private static MapView fakeView() {
        return (MapView) Proxy.newProxyInstance(MapView.class.getClassLoader(), new Class<?>[]{MapView.class},
                (self, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    /** Records the argument of each setter a label calls, by setter name. */
    private static final class FakeMeta implements ArmsMapItem.Target {
        final Map<String, Object> calls = new HashMap<>();

        @Override
        public void setMapView(MapView view) {
            calls.put("setMapView", view);
        }

        @Override
        public void setDisplayName(String name) {
            calls.put("setDisplayName", name);
        }

        @Override
        public void setLore(List<String> lore) {
            calls.put("setLore", lore);
        }
    }
}
