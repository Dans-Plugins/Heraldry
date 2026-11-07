package com.dansplugins.heraldry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Plugin entry point. Loads stored arms and issued maps, re-binds each issued map to its owner's
 * arms, and registers {@code /heraldry}. The remaining features are scoped in MVP.md.
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
        MapRasteriser rasteriser = new MapRasteriser();
        MapRebinder rebinder = new MapRebinder(armsStore, rasteriser, Bukkit::getMap);
        MapRebinder.Result result = rebinder.rebind(issuedMaps);
        for (String problem : result.getProblems()) {
            getLogger().warning(problem);
        }
        getLogger().info("Re-bound arms to " + result.getRebound() + " issued map(s).");

        Path importFolder = dataFolder.resolve("import");
        try {
            Files.createDirectories(importFolder);
        } catch (IOException e) {
            getLogger().warning("Could not create the import folder: " + e.getMessage());
        }
        HeraldryCommand command = new HeraldryCommand(new ArmsImporter(importFolder, armsStore), armsStore,
                rasteriser, issuedMaps, rebinder, new ServerEnvironment(), getLogger());
        getCommand("heraldry").setExecutor(command);
        getCommand("heraldry").setTabCompleter(command);
    }

    @Override
    public void onDisable() {
    }

    /** The running server behind {@link HeraldryCommand}. */
    private static final class ServerEnvironment implements HeraldryCommand.Environment {

        @Override
        public HeraldryCommand.Bearer senderAsPlayer(CommandSender sender) {
            return sender instanceof Player ? new OnlinePlayer((Player) sender) : null;
        }

        @Override
        public HeraldryCommand.Bearer findPlayer(String name) {
            Player player = Bukkit.getPlayerExact(name);
            return player == null ? null : new OnlinePlayer(player);
        }

        @Override
        public List<String> playerNames() {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }

        @Override
        public void showToPlayers(MapView view) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMap(view);
            }
        }
    }

    /** An online player, as {@link HeraldryCommand} sees one. */
    private static final class OnlinePlayer implements HeraldryCommand.Bearer {
        private final Player player;

        OnlinePlayer(Player player) {
            this.player = player;
        }

        @Override
        public UUID getUniqueId() {
            return player.getUniqueId();
        }

        @Override
        public String getName() {
            return player.getName();
        }

        @Override
        public MapView createMap() {
            return Bukkit.createMap(player.getWorld());
        }

        @Override
        public void give(MapView view, ArmsOwner owner) {
            ItemStack item = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) item.getItemMeta();
            ArmsMapItem.label(meta, view, owner, player.getName());
            item.setItemMeta(meta);
            // A full inventory hands back what did not fit; drop it so an issued map is never lost.
            for (ItemStack leftOver : player.getInventory().addItem(item).values()) {
                player.getWorld().dropItem(player.getLocation(), leftOver);
            }
        }
    }
}
