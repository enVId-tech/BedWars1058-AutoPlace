package dev.etran.model

import java.util.Locale

/** The ten selectable gamemodes: `teams` teams of `size` players each. [icon] is an item spec (see Items). */
enum class GameModeType(val label: String, val teams: Int, val size: Int, val icon: String) {
    T1V1V1V1("1v1v1v1", 4, 1, "IRON_INGOT"),
    T2V2V2V2("2v2v2v2", 4, 2, "GOLD_INGOT"),
    T3V3V3V3("3v3v3v3", 4, 3, "DIAMOND"),
    T4V4V4V4("4v4v4v4", 4, 4, "EMERALD"),
    T1V1("1v1", 2, 1, "IRON_NUGGET|IRON_INGOT"),
    T2V2("2v2", 2, 2, "GOLD_NUGGET"),
    T3V3("3v3", 2, 3, "DIAMOND_BLOCK"),
    T4V4("4v4", 2, 4, "EMERALD_BLOCK"),
    SOLOS("solos", 8, 1, "IRON_BLOCK"),
    DOUBLES("doubles", 8, 2, "GOLD_BLOCK");

    companion object {
        @JvmStatic
        fun parse(s: String?): GameModeType? {
            if (s == null) return null
            val l = s.lowercase(Locale.ROOT)
            return entries.firstOrNull { it.label == l }
        }
    }
}
