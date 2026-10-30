package com.dansplugins.heraldry;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Plugin entry point. Loads stored arms and issued maps, and re-binds each issued map to its
 * owner's arms. The remaining features are scoped in MVP.md.
 */
public final class Heraldry extends JavaPlugin {

    private ArmsStore armsStore;
    private IssuedMaps issuedMaps;

    @Override
    @SuppressWarnings("deprecation")
    public void onEnable() {
        Path dataFolder = getDataFolder().toPath();
        armsStore = new ArmsStore(dataFolder.resolve("arms"));
        try {
            issuedMaps = IssuedMaps.load(dataFolder.resolve("issued-maps.txt"));
        } catch (IOException e) {
            // Carrying on would let the next issued map overwrite the records that could not be read.
            getLogger().severe("Could not read the issued maps, so Heraldry is disabled: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        MapRebinder.Result result = new MapRebinder(armsStore, new MapRasteriser(), Bukkit::getMap).rebind(issuedMaps);
        for (String problem : result.getProblems()) {
            getLogger().warning(problem);
        }
        getLogger().info("Re-bound arms to " + result.getRebound() + " issued map(s).");
    }

    @Override
    public void onDisable() {
    }
}
