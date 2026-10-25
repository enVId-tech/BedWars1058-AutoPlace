package dev.etran.model;

import org.bukkit.Material;

import java.util.Locale;

/** The eight selectable gamemodes: {@code teams} teams of {@code size} players each. */
public enum GameModeType {
    T1V1V1V1("1v1v1v1", 4, 1, Material.IRON_INGOT),
    T2V2V2V2("2v2v2v2", 4, 2, Material.GOLD_INGOT),
    T3V3V3V3("3v3v3v3", 4, 3, Material.DIAMOND),
    T4V4V4V4("4v4v4v4", 4, 4, Material.EMERALD),
    T1V1("1v1", 2, 1, Material.IRON_NUGGET),
    T2V2("2v2", 2, 2, Material.GOLD_NUGGET),
    T3V3("3v3", 2, 3, Material.DIAMOND_BLOCK),
    T4V4("4v4", 2, 4, Material.EMERALD_BLOCK);

    public final String label;
    public final int teams;
    public final int size;
    public final Material icon;

    GameModeType(String label, int teams, int size, Material icon) {
        this.label = label;
        this.teams = teams;
        this.size = size;
        this.icon = icon;
    }

    public static GameModeType parse(String s) {
        if (s == null) return null;
        String l = s.toLowerCase(Locale.ROOT);
        for (GameModeType g : values()) {
            if (g.label.equals(l)) return g;
        }
        return null;
    }
}
