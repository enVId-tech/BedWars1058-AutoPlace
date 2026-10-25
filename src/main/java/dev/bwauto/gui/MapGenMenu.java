package dev.etran.gui;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.bw.ActionQueue;
import dev.etran.bw.ElementPlacer;
import dev.etran.model.Element;
import dev.etran.place.AutoRunner;
import dev.etran.place.NextGen;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class MapGenMenu extends Menu {
    public MapGenMenu(BwAutoPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Map Generators";
    }

    @Override
    protected int rows() {
        return 3;
    }

    private void gen(int slot, Material icon, Element e) {
        int n = session.mapCount(e);
        set(slot, Items.of(icon, (n > 0 ? "&a✔ " : "&c✘ ") + e.label,
                "&7Placed: &f" + n, "&eClick to place at your position"),
                c -> ElementPlacer.placeHere(plugin, player, session, null, e, this::refresh));
    }

    @Override
    protected void render() {
        fill(Material.GRAY_STAINED_GLASS_PANE);
        gen(10, Material.DIAMOND, Element.GEN_DIAMOND);
        gen(11, Material.EMERALD, Element.GEN_EMERALD);

        set(13, Items.of(Material.DIAMOND_BLOCK, "&bAuto-place all generators",
                "&7Finds every diamond/emerald block that has",
                "&7stairs on all four sides, three air blocks",
                "&7above and no generator yet, and places it."),
                e -> placeAll());

        double deg = plugin.settings().defaultRotation();
        set(15, Items.of(Material.CLOCK, "&6Rotate around map centre (" + (long) deg + "°)",
                "&7Moves you to the same spot on the next",
                "&7island. Use &f/nextgen <degrees>&7 for any angle,",
                "&7or &f/nextgen diamond|emerald [degrees]&7 to",
                "&7detect and place one at a time."),
                e -> NextGen.rotate(plugin, player, deg));

        set(18, Items.of(Material.RED_CONCRETE, "&c&lBack"), e -> go(new MainMenu(plugin, player)));
    }

    private void placeAll() {
        if (session.busy) {
            Msg.err(player, "Another placement is still running - wait a moment.");
            return;
        }
        Location back = player.getLocation().clone();
        ActionQueue q = new ActionQueue(plugin, player);
        int[] n = AutoRunner.queueGenerators(plugin, player, session, q, player.getWorld(), back);
        if (n[0] + n[1] == 0) return;
        player.closeInventory();
        session.busy = true;
        q.teleport(back);
        q.done(() -> {
            session.busy = false;
            Msg.ok(player, "Placed " + n[0] + " diamond and " + n[1] + " emerald generator(s).");
        });
        q.start();
    }
}
