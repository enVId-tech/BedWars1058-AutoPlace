package dev.etran.model;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

/** The eight Bedwars1058 team colours, with the dye names of the wool that represents them. */
public enum TeamColor {
    RED("Red", ChatColor.RED, "RED"),
    BLUE("Blue", ChatColor.BLUE, "BLUE"),
    GREEN("Green", ChatColor.GREEN, "LIME", "GREEN"),
    YELLOW("Yellow", ChatColor.YELLOW, "YELLOW"),
    AQUA("Aqua", ChatColor.AQUA, "CYAN", "LIGHT_BLUE"),
    WHITE("White", ChatColor.WHITE, "WHITE"),
    PINK("Pink", ChatColor.LIGHT_PURPLE, "PINK"),
    GRAY("Gray", ChatColor.GRAY, "GRAY");

    public final String display;
    public final ChatColor chat;
    private final String[] dyes;

    TeamColor(String display, ChatColor chat, String... dyes) {
        this.display = display;
        this.chat = chat;
        this.dyes = dyes;
    }

    /** Name passed to Bedwars1058 as the team name. */
    public String bwName() {
        return display;
    }

    public boolean matchesDye(String dye) {
        for (String d : dyes) {
            if (d.equals(dye)) return true;
        }
        return false;
    }

    public Material wool() {
        Material m = Material.matchMaterial(dyes[0] + "_WOOL");
        return m != null ? m : Material.WHITE_WOOL;
    }

    public Material bed() {
        Material m = Material.matchMaterial(dyes[0] + "_BED");
        return m != null ? m : Material.RED_BED;
    }

    public static TeamColor fromName(String s) {
        if (s == null) return null;
        String u = s.toUpperCase(Locale.ROOT);
        for (TeamColor c : values()) {
            if (c.name().equals(u) || c.display.equalsIgnoreCase(s)) return c;
        }
        return null;
    }

    public static TeamColor fromDye(String dye) {
        for (TeamColor c : values()) {
            if (c.matchesDye(dye)) return c;
        }
        return null;
    }

    /** Default team set for a game with the given number of teams (maps are symmetric: 2 or 4 bases). */
    public static List<TeamColor> defaults(int teams) {
        return teams <= 2 ? List.of(RED, BLUE) : List.of(RED, BLUE, GREEN, YELLOW);
    }
}
