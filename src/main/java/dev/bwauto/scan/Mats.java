package dev.etran.scan;

import org.bukkit.Material;

import java.util.List;

/** Material helpers that work off names so they keep working across Minecraft versions. */
public final class Mats {
    private static final List<String> DYES = List.of(
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK");

    private Mats() {
    }

    public static boolean isBed(Material m) {
        return m.name().endsWith("_BED");
    }

    public static boolean isStairs(Material m) {
        return m.name().endsWith("_STAIRS");
    }

    /** A block an entity can stand inside. */
    public static boolean passable(Material m) {
        return !m.isSolid();
    }

    /** Material name without a leading dye colour, so "RED_WOOL" and "BLUE_WOOL" compare equal. */
    public static String family(Material m) {
        String n = m.name();
        for (String d : DYES) {
            if (n.startsWith(d + "_")) return n.substring(d.length() + 1);
        }
        return n;
    }

    /** The dye colour prefix of a material name, or null. */
    public static String dyeOf(Material m, String suffix) {
        String n = m.name();
        if (!n.endsWith(suffix)) return null;
        String dye = n.substring(0, n.length() - suffix.length());
        return DYES.contains(dye) ? dye : null;
    }
}
