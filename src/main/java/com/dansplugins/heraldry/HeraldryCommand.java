package com.dansplugins.heraldry;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.map.MapView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The {@code /heraldry} command: view, set and issue a player's arms.
 *
 * <p>Each subcommand acts on the sender, or on an online player named in the command. The console
 * has no arms of its own, so it must name a player. Faction arms are not offered yet.
 */
public final class HeraldryCommand implements CommandExecutor, TabCompleter {

    /** The subcommands, in the order they are completed. Each has the permission {@code heraldry.<name>}. */
    static final List<String> SUBCOMMANDS = Collections.unmodifiableList(Arrays.asList("view", "set", "issue"));

    private static final String VIEW_USAGE = "/heraldry view [player]";
    private static final String SET_USAGE = "/heraldry set <file> [player]";
    private static final String ISSUE_USAGE = "/heraldry issue [player]";

    /**
     * What the command needs from a running server. {@code Player} itself cannot be faked in a test:
     * loading a proxy of it initialises {@code Sound}, which needs a running server.
     */
    public interface Environment {
        /** Returns the player who sent the command, or null if the sender is not a player. */
        Bearer senderAsPlayer(CommandSender sender);

        /** Returns the online player with exactly this name, or null if there is none. */
        Bearer findPlayer(String name);

        /** Returns the names of the online players. */
        List<String> playerNames();

        /** Sends the map's current pixels to every online player, as a client keeps those it last received. */
        void showToPlayers(MapView view);
    }

    /** The parts of an online player the command uses. */
    public interface Bearer {
        UUID getUniqueId();

        String getName();

        /** Creates a new map view for arms that will be given to this player. */
        MapView createMap();

        /** Gives this player a filled map showing the view, labelled with the owner's arms. */
        void give(MapView view, ArmsOwner owner);
    }

    private final ArmsImporter importer;
    private final ArmsStore store;
    private final MapRasteriser rasteriser;
    private final IssuedMaps issuedMaps;
    private final MapRebinder rebinder;
    private final Environment environment;
    private final Logger logger;

    public HeraldryCommand(ArmsImporter importer, ArmsStore store, MapRasteriser rasteriser, IssuedMaps issuedMaps,
                           MapRebinder rebinder, Environment environment, Logger logger) {
        if (importer == null || store == null || rasteriser == null || issuedMaps == null || rebinder == null
                || environment == null || logger == null) {
            throw new IllegalArgumentException("Every HeraldryCommand argument is required");
        }
        this.importer = importer;
        this.store = store;
        this.rasteriser = rasteriser;
        this.issuedMaps = issuedMaps;
        this.rebinder = rebinder;
        this.environment = environment;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (!SUBCOMMANDS.contains(subcommand)) {
            sender.sendMessage("Usage: " + VIEW_USAGE + ", " + SET_USAGE + " or " + ISSUE_USAGE);
            return true;
        }
        if (!sender.hasPermission(permission(subcommand))) {
            sender.sendMessage("You do not have permission to use /heraldry " + subcommand + ".");
            return true;
        }
        switch (subcommand) {
            case "view":
                view(sender, args);
                break;
            case "set":
                set(sender, args);
                break;
            default:
                issue(sender, args);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> allowed = new ArrayList<>();
            for (String subcommand : SUBCOMMANDS) {
                if (sender.hasPermission(permission(subcommand))) {
                    allowed.add(subcommand);
                }
            }
            return matching(allowed, args[0]);
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (!SUBCOMMANDS.contains(subcommand) || !sender.hasPermission(permission(subcommand))) {
            return Collections.emptyList();
        }
        String last = args[args.length - 1];
        if (subcommand.equals("set") && args.length == 2) {
            return matching(importableFiles(), last);
        }
        int playerArgument = subcommand.equals("set") ? 3 : 2;
        return args.length == playerArgument ? matching(environment.playerNames(), last) : Collections.emptyList();
    }

    private void view(CommandSender sender, String[] args) {
        if (args.length > 2) {
            sender.sendMessage("Usage: " + VIEW_USAGE);
            return;
        }
        Bearer target = target(sender, args.length == 2 ? args[1] : null, VIEW_USAGE);
        if (target == null) {
            return;
        }
        boolean hasArms = store.has(ArmsOwner.player(target.getUniqueId()));
        sender.sendMessage(target.getName() + (hasArms ? " has arms on record." : " has no arms."));
    }

    private void set(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage("Usage: " + SET_USAGE);
            return;
        }
        Bearer target = target(sender, args.length == 3 ? args[2] : null, SET_USAGE);
        if (target == null) {
            return;
        }
        ArmsOwner owner = ArmsOwner.player(target.getUniqueId());
        try {
            importer.importArms(owner, args[1]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(e.getMessage());
            return;
        } catch (IOException e) {
            logger.warning("Could not import " + args[1] + " for " + target.getName() + ": " + e.getMessage());
            sender.sendMessage("Could not set those arms; see the server log.");
            return;
        }
        redrawIssuedMaps(owner);
        sender.sendMessage(target.getName() + "'s arms are now " + args[1] + ".");
    }

    /** Makes the maps already issued to the owner draw their new arms, without waiting for a restart. */
    private void redrawIssuedMaps(ArmsOwner owner) {
        MapRebinder.Result result = rebinder.rebind(issuedMaps, owner);
        for (String problem : result.getProblems()) {
            logger.warning(problem);
        }
        for (MapView view : result.getMaps()) {
            environment.showToPlayers(view);
        }
    }

    private void issue(CommandSender sender, String[] args) {
        if (args.length > 2) {
            sender.sendMessage("Usage: " + ISSUE_USAGE);
            return;
        }
        Bearer target = target(sender, args.length == 2 ? args[1] : null, ISSUE_USAGE);
        if (target == null) {
            return;
        }
        ArmsOwner owner = ArmsOwner.player(target.getUniqueId());
        ArmsMapIssuer issuer = new ArmsMapIssuer(store, rasteriser, issuedMaps, target::createMap);
        Optional<MapView> view;
        try {
            view = issuer.issue(owner);
        } catch (IOException e) {
            logger.warning("Could not issue the arms of " + target.getName() + ": " + e.getMessage());
            sender.sendMessage("Could not issue those arms; see the server log.");
            return;
        }
        if (!view.isPresent()) {
            sender.sendMessage(target.getName() + " has no arms to issue.");
            return;
        }
        target.give(view.get(), owner);
        sender.sendMessage("Gave " + target.getName() + " a map of their arms.");
    }

    /** Returns the named player, or the sender if no name was given. Reports why if there is no such player. */
    private Bearer target(CommandSender sender, String name, String usage) {
        if (name != null) {
            Bearer named = environment.findPlayer(name);
            if (named == null) {
                sender.sendMessage("There is no online player named " + name + ".");
            }
            return named;
        }
        Bearer self = environment.senderAsPlayer(sender);
        if (self == null) {
            sender.sendMessage("The console must name a player: " + usage);
        }
        return self;
    }

    private List<String> importableFiles() {
        try {
            return importer.list();
        } catch (IOException e) {
            logger.warning("Could not list the import folder: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static List<String> matching(List<String> candidates, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private static String permission(String subcommand) {
        return "heraldry." + subcommand;
    }
}
