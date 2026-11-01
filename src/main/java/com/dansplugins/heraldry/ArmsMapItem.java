package com.dansplugins.heraldry;

import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.Arrays;
import java.util.List;

/**
 * Labels a filled map item as one bearing an owner's arms.
 *
 * <p>The item's meta is given the issued map view, a display name and a lore tag. Creating the
 * {@code FILLED_MAP} item and reading its meta needs a running server's item factory, so that is
 * left to the caller:
 *
 * <pre>{@code
 * ItemStack item = new ItemStack(Material.FILLED_MAP);
 * MapMeta meta = (MapMeta) item.getItemMeta();
 * ArmsMapItem.label(meta, view, owner, ownerName);
 * item.setItemMeta(meta);
 * }</pre>
 *
 * <p>The label is for players to read. Which owner a map belongs to is recorded in
 * {@link IssuedMaps} against the map id, so renaming the item or editing its lore changes nothing.
 */
public final class ArmsMapItem {

    /** The last lore line of every map issued by Heraldry. */
    static final String LORE_TAG = "Issued by Heraldry";

    private ArmsMapItem() {
    }

    /** The display name of a map bearing the arms of the named owner. */
    public static String displayName(String ownerName) {
        return "Arms of " + checkName(ownerName);
    }

    /** The lore of a map bearing the owner's arms: what kind of owner it is, then the Heraldry tag. */
    public static List<String> lore(ArmsOwner owner) {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        String kind = owner.getType() == ArmsOwner.Type.FACTION ? "Arms of a faction" : "Arms of a player";
        return Arrays.asList(kind, LORE_TAG);
    }

    /**
     * Points the meta at the issued map view and gives it the owner's display name and lore.
     *
     * @param ownerName the name players know the owner by, such as a player or faction name
     */
    public static void label(MapMeta meta, MapView view, ArmsOwner owner, String ownerName) {
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        label(new Target() {
            @Override
            public void setMapView(MapView mapView) {
                meta.setMapView(mapView);
            }

            @Override
            public void setDisplayName(String name) {
                meta.setDisplayName(name);
            }

            @Override
            public void setLore(List<String> lore) {
                meta.setLore(lore);
            }
        }, view, owner, ownerName);
    }

    /**
     * The parts of {@link MapMeta} a label sets. {@code MapMeta} itself cannot be faked in a test:
     * loading an implementation of it initialises {@code Enchantment}, which needs a running server.
     */
    interface Target {
        void setMapView(MapView view);

        void setDisplayName(String name);

        void setLore(List<String> lore);
    }

    /** Every argument is checked before anything is set, so invalid arguments leave the target untouched. */
    static void label(Target target, MapView view, ArmsOwner owner, String ownerName) {
        if (target == null || view == null) {
            throw new IllegalArgumentException("meta and view must not be null");
        }
        String name = displayName(ownerName);
        List<String> lore = lore(owner);
        target.setMapView(view);
        target.setDisplayName(name);
        target.setLore(lore);
    }

    private static String checkName(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("ownerName must not be blank");
        }
        return ownerName.trim();
    }
}
