package dev.etran.gui;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.bw.ActionQueue;
import dev.etran.bw.ElementPlacer;
import dev.etran.model.Element;
import dev.etran.place.AutoRunner;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class LobbyMenu extends Menu {
    public LobbyMenu(BwAutoPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Lobby & Boundary";
    }

    @Override
    protected int rows() {
        return 3;
    }

    private void marker(int slot, Material icon, Element e, String hint) {
        boolean placed = session.mapPlaced(e);
        set(slot, Items.of(icon, (placed ? "&a✔ " : "&c✘ ") + e.label,
                "&7" + hint, "&eClick to place at your position"),
                c -> ElementPlacer.placeHere(plugin, player, session, null, e, this::refresh));
    }

    @Override
    protected void render() {
        fill(Material.GRAY_STAINED_GLASS_PANE);
        marker(10, Material.ENDER_PEARL, Element.WAITING_SPAWN, "Where players wait before the game.");
        marker(11, Material.SPECTRAL_ARROW, Element.SPECTATOR_SPAWN, "Where spectators appear.");
        marker(12, Material.WOODEN_AXE, Element.LOBBY_POS1, "First corner of the region removed at start.");
        marker(13, Material.STONE_AXE, Element.LOBBY_POS2, "Opposite corner of that region.");

        set(15, Items.of(Material.REPEATER, "&6Boundary: " + (session.autoLobby ? "&aAutomatic" : "&eManual"),
                "&7Semi-Automatic mode: should automatic",
                "&7placement detect the lobby for you?",
                "&8Fully Automatic mode always detects it.",
                "&eClick to toggle"),
                e -> {
                    session.autoLobby = !session.autoLobby;
                    refresh();
                });

        set(16, Items.of(Material.ENDER_EYE, "&dDetect lobby structure now",
                "&7Scans around the map centre, builds a",
                "&7bounding box and puts both spawns at its",
                "&7exact centre."),
                e -> detectNow());

        set(18, Items.of(Material.RED_CONCRETE, "&c&lBack"), e -> go(new MainMenu(plugin, player)));
        set(26, Items.of(Material.LIME_CONCRETE, "&a&lTeam & Base Setup ➜"), e -> {
            if (session.gamemode == null) {
                Msg.err(player, "Pick a gamemode first.");
                return;
            }
            if (session.selected == null) session.selected = session.requiredTeams().get(0);
            go(new BaseMenu(plugin, player));
        });
    }

    private void detectNow() {
        if (session.busy) {
            Msg.err(player, "Another placement is still running - wait a moment.");
            return;
        }
        ActionQueue q = new ActionQueue(plugin, player);
        Location back = player.getLocation().clone();
        if (!AutoRunner.queueLobbyDetection(plugin, player, session, q, player.getWorld())) return;
        player.closeInventory();
        session.busy = true;
        q.teleport(back);
        q.done(() -> {
            session.busy = false;
            Msg.ok(player, "Lobby spawns and region placed.");
        });
        q.start();
    }
}
