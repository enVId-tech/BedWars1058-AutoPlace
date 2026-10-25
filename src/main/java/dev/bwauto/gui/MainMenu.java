package dev.etran.gui;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.model.BedInfo;
import dev.etran.model.SetupMode;
import dev.etran.model.TeamColor;
import dev.etran.place.AutoRunner;
import dev.etran.place.TeamManager;
import dev.etran.place.Validator;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class MainMenu extends Menu {
    public MainMenu(BwAutoPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Bedwars Auto Setup";
    }

    @Override
    protected int rows() {
        return 3;
    }

    /** Shared by the main and base menus: the semi/auto action button. */
    static void automationButton(Menu m, int slot, BwAutoPlugin plugin, Player player, SetupMode mode) {
        switch (mode) {
            case MANUAL -> m.set(slot, Items.of(Material.BARRIER, "&7Automation off",
                    "&7Manual mode only runs the Bedwars", "&7command where you stand.",
                    "&7Change the mode to enable automation."), null);
            case SEMI -> m.set(slot, Items.of(Material.ARROW, "&bApply to next base",
                    "&7Copies the first base onto the nearest",
                    "&7unassigned bed, then walks you to the",
                    "&7bed after it."),
                    e -> AutoRunner.request(plugin, player, true));
            case AUTO -> m.set(slot, Items.of(Material.ENDER_EYE, "&dRun automatic placement",
                    "&7Copies the first base onto every other",
                    "&7bed, detects the lobby and places all",
                    "&7diamond and emerald generators."),
                    e -> AutoRunner.request(plugin, player, false));
        }
    }

    @Override
    protected void render() {
        fill(Material.GRAY_STAINED_GLASS_PANE);
        List<TeamColor> req = session.requiredTeams();
        long missingTeams = req.stream().filter(c -> !session.hasTeam(c)).count();

        // Red concrete on the left: the auto-team creator.
        List<String> teamLore = new ArrayList<>();
        teamLore.add("&7Creates every team the gamemode needs.");
        if (session.gamemode == null) {
            teamLore.add("&cPick a gamemode first.");
        } else {
            teamLore.add(missingTeams == 0 ? "&aAll " + req.size() + " teams exist." : "&eMissing: " + missingTeams + " of " + req.size());
        }
        set(9, Items.of(Material.RED_CONCRETE, "&c&lAuto-Create Teams", teamLore),
                e -> TeamManager.autoCreate(plugin, player, session, this::refresh));

        set(11, Items.of(Material.NETHER_STAR, "&6Gamemode",
                "&7Current: &f" + (session.gamemode == null ? "none" : session.gamemode.label),
                "&7Changing to fewer teams deletes and",
                "&7recreates the existing teams."),
                e -> go(new GamemodeMenu(plugin, player)));

        set(12, Items.of(Material.COMPARATOR, "&6Setup mode: &f" + session.mode.label,
                "&7Left-click: next mode  Right-click: previous",
                "&8Manual: ArenaSetup style",
                "&8Semi-Automatic: base by base",
                "&8Fully Automatic: algorithmic"),
                e -> {
                    session.mode = e.isRightClick() ? session.mode.prev() : session.mode.next();
                    refresh();
                });

        set(13, Items.of(Material.COMPASS, "&6Lobby & Boundary",
                "&7Player/spectator spawns and the lobby",
                "&7region that is removed at game start."),
                e -> go(new LobbyMenu(plugin, player)));

        set(14, Items.of(Material.DIAMOND_BLOCK, "&6Map Generators",
                "&7Diamond and emerald generators, plus",
                "&7rotation around the map centre."),
                e -> go(new MapGenMenu(plugin, player)));

        automationButton(this, 15, plugin, player, session.mode);

        List<String> status = new ArrayList<>();
        status.add("&7Mode: &f" + session.mode.label);
        status.add("&7Gamemode: &f" + (session.gamemode == null ? "none" : session.gamemode.label));
        BedInfo bed = session.template.bed();
        status.add("&7Template base: &f" + (session.template.team() == null ? "none" : session.template.team().display)
                + (bed == null ? " &c(no bed yet)" : " &a(bed found)"));
        List<String> missing = Validator.missingAll(session, plugin.settings());
        status.add(missing.isEmpty() ? "&aNothing missing." : "&eStill missing: &f" + missing.size() + " item(s)");
        set(4, Items.of(Material.BOOK, "&eStatus", status), null);

        set(17, Items.of(Material.LIME_CONCRETE, "&a&lTeam & Base Setup ➜",
                "&7Spawns, generators, shops, drop areas."), e -> {
            if (session.gamemode == null) {
                Msg.err(player, "Pick a gamemode first.");
                return;
            }
            if (session.selected == null) session.selected = session.requiredTeams().get(0);
            go(new BaseMenu(plugin, player));
        });
    }
}
