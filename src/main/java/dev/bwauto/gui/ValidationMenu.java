package dev.etran.gui;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.model.Element;
import dev.etran.model.TeamColor;
import dev.etran.place.TeamManager;
import dev.etran.place.Validator;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Final validation. Every element of every team and the map is shown as present (green) or missing (red).
 * Clicking an element jumps to the menu where it can be placed manually; red concrete goes back; green
 * concrete saves. If something is missing the first click on green only lists what is missing and the
 * second click (within 10 seconds) saves anyway.
 */
public final class ValidationMenu extends Menu {
    private static final long CONFIRM_WINDOW_MS = 10_000L;

    public ValidationMenu(BwAutoPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Final Validation";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        fill(Material.GRAY_STAINED_GLASS_PANE);
        List<String> missing = Validator.missingAll(session, plugin.settings());
        boolean complete = missing.isEmpty();

        set(4, Items.of(complete ? Material.LIME_DYE : Material.RED_DYE,
                complete ? "&a&lEverything is placed" : "&c&lSomething is missing",
                complete ? List.of("&7Ready to save.")
                        : List.of("&7Click any red item to place it,", "&7or save anyway with the green button.")),
                null);

        // Teams, one row each (rows 1-4).
        List<TeamColor> req = session.requiredTeams();
        for (int i = 0; i < req.size() && i < 4; i++) {
            TeamColor c = req.get(i);
            int row = 9 * (i + 1);
            boolean exists = session.hasTeam(c);
            set(row, Items.of(c.wool(), c.chat + c.display,
                    exists ? "&7Team exists" : "&cTeam not created - click to create"),
                    e -> {
                        if (!session.hasTeam(c)) {
                            TeamManager.autoCreate(plugin, player, session, this::refresh);
                        } else {
                            session.selected = c;
                            go(new BaseMenu(plugin, player));
                        }
                    });
            List<Element> te = Element.teamElements();
            for (int k = 0; k < te.size() && k < 7; k++) {
                Element el = te.get(k);
                boolean ok = session.teamPlaced(c, el);
                set(row + 1 + k, Items.of(ok ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                        (ok ? "&a✔ " : "&c✘ ") + el.label,
                        ok ? "&7Placed (click to edit)" : "&eMissing - click to place"),
                        e -> {
                            if (!session.hasTeam(c)) {
                                TeamManager.requireTeam(player, session, c);
                                return;
                            }
                            session.selected = c;
                            go(new BaseMenu(plugin, player));
                        });
            }
        }

        // Map elements (bottom row).
        List<Element> me = Element.mapElements();
        for (int k = 0; k < me.size() && k < 6; k++) {
            Element el = me.get(k);
            boolean ok = session.mapPlaced(el);
            set(46 + k, Items.of(ok ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                    (ok ? "&a✔ " : "&c✘ ") + el.label,
                    ok ? "&7Placed (click to edit)" : "&eMissing - click to place"),
                    e -> go(el.isGenerator() ? new MapGenMenu(plugin, player) : new LobbyMenu(plugin, player)));
        }

        set(45, Items.of(Material.RED_CONCRETE, "&c&lBack"), e -> go(new BaseMenu(plugin, player)));

        boolean armed = !complete && System.currentTimeMillis() < session.saveConfirmUntil;
        String name = complete ? "&a&lConfirm & Save"
                : armed ? "&a&lClick again to save anyway" : "&a&lSave anyway...";
        set(53, Items.of(Material.LIME_CONCRETE, name,
                complete ? List.of("&7Runs the Bedwars save command.")
                        : List.of("&cThe arena is incomplete.", "&7First click lists what is missing.")),
                e -> confirm());
    }

    private void confirm() {
        List<String> missing = Validator.missingAll(session, plugin.settings());
        if (missing.isEmpty()) {
            save();
            return;
        }
        if (System.currentTimeMillis() < session.saveConfirmUntil) {
            save();
            return;
        }
        session.saveConfirmUntil = System.currentTimeMillis() + CONFIRM_WINDOW_MS;
        player.sendMessage(Msg.PREFIX + ChatColor.RED + "The arena is incomplete. Missing:");
        for (String line : missing) player.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + line);
        player.sendMessage(Msg.PREFIX + ChatColor.YELLOW + "Click the red items to place them, the red concrete to go "
                + "back, or click the green concrete again within 10 seconds to save anyway.");
        refresh();
    }

    private void save() {
        player.closeInventory();
        String cmd = plugin.bw().build("save");
        if (cmd == null) {
            Msg.err(player, "No save command is configured (bw-commands.save in config.yml).");
            return;
        }
        player.performCommand(cmd);
        Msg.ok(player, "Save command sent: /" + cmd);
        plugin.sessions().remove(player.getUniqueId());
    }
}
