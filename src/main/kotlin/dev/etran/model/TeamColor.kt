package dev.etran.model

import dev.etran.compat.Dyes
import org.bukkit.ChatColor
import java.util.Locale

/** The eight Bedwars1058 team colours, with the dye names of the wool that represents them. */
enum class TeamColor(val display: String, val chat: ChatColor, private vararg val dyes: String) {
    RED("Red", ChatColor.RED, "RED"),
    BLUE("Blue", ChatColor.BLUE, "BLUE"),
    GREEN("Green", ChatColor.GREEN, "LIME", "GREEN"),
    YELLOW("Yellow", ChatColor.YELLOW, "YELLOW"),
    AQUA("Aqua", ChatColor.AQUA, "CYAN", "LIGHT_BLUE"),
    WHITE("White", ChatColor.WHITE, "WHITE"),
    PINK("Pink", ChatColor.LIGHT_PURPLE, "PINK"),
    GRAY("Gray", ChatColor.GRAY, "GRAY");

    /** Name passed to Bedwars1058 as the team name. */
    fun bwName(): String = display

    fun matchesDye(dye: String?): Boolean = dyes.any { it == dye }

    /** Item spec (see Items) for this colour's wool on any version. */
    fun woolSpec(): String = dyes[0] + "_WOOL|WOOL:" + Dyes.dataOf(dyes[0])

    /** Item spec for this colour's bed on any version. */
    fun bedSpec(): String = dyes[0] + "_BED|BED:" + Dyes.dataOf(dyes[0])

    companion object {
        @JvmStatic
        fun fromName(s: String?): TeamColor? {
            if (s == null) return null
            val u = s.uppercase(Locale.ROOT)
            return entries.firstOrNull { it.name == u || it.display.equals(s, ignoreCase = true) }
        }

        @JvmStatic
        fun fromDye(dye: String?): TeamColor? = entries.firstOrNull { it.matchesDye(dye) }

        /** Default team set for a game with 2, 4 or 8 teams (maps are symmetric). The 4-team set is a prefix of the 8-team set. */
        @JvmStatic
        fun defaults(teams: Int): List<TeamColor> = when {
            teams <= 2 -> listOf(RED, BLUE)
            teams <= 4 -> listOf(RED, BLUE, GREEN, YELLOW)
            else -> listOf(RED, BLUE, GREEN, YELLOW, AQUA, WHITE, PINK, GRAY)
        }
    }
}
