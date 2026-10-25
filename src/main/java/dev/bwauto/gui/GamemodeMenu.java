package dev.etran.gui;

import dev.etran.BwAutoPlugin;
import dev.etran.model.GameModeType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class GamemodeMenu extends Menu {
    // Row 1: four-team modes, row 2: two-team modes.
    private static final GameModeType[] ORDER = {
            GameModeType.T1V1V1V1, GameModeType.T2V2V2V2, GameModeType.T3V3V3V3, GameModeType.T4V4V4V4,
            GameModeType.T1V1, GameModeType.T2V2, GameModeType.T3V3, GameModeType.T4V4};
    private static final int[] SLOTS = {10, 11, 12, 13, 19, 20, 21, 22};

    public GamemodeMenu(BwAutoPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Select Gamemode";
    }

    @Override
    protected int rows() {
        return 4;
    }

    @Override
    protected void render() {
        fill(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < ORDER.length; i++) {
            GameModeType g = ORDER[i];
            boolean current = g == session.gamemode;
            set(SLOTS[i], Items.of(g.icon, (current ? "&a▶ " : "&f") + g.label,
                    "&7" + g.teams + " teams of " + g.size + " player" + (g.size == 1 ? "" : "s"),
                    current ? "&aCurrently selected" : "&eClick to select"),
                    e -> {
                        if (g == session.gamemode) return;
                        dev.etran.place.TeamManager.applyGamemode(plugin, player, session, g, this::refresh);
                    });
        }
        set(27, Items.of(Material.RED_CONCRETE, "&c&lBack"), e -> go(new MainMenu(plugin, player)));
    }
}
