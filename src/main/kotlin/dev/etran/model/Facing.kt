package dev.etran.model

import java.util.Locale

/**
 * Pure facing maths for shops and upgrades (no server types, so it is unit-testable).
 *
 * Minecraft yaw: 0 = south (+Z), 90 = west, 180 = north, 270 = east. A shop placed from a hotbar tool
 * defaults to looking back at the player: the opposite of the direction the player is facing, snapped to
 * 45 degree steps. That way a shop placed from outside a booth looks into the open, not at the wall.
 */
object Facing {
    enum class Mode(@JvmField val label: String) {
        /** Opposite of the player's facing: the NPC looks back at the player. */
        TOWARD_PLAYER("toward"),

        /** Same direction the player is facing (the old behaviour). */
        SAME_AS_PLAYER("same"),

        /** A fixed yaw regardless of the player. */
        FIXED("fixed");

        companion object {
            @JvmStatic
            fun parse(s: String?): Mode? {
                if (s == null) return null
                val l = s.lowercase(Locale.ROOT)
                return when {
                    l.startsWith("toward") || l == "player" || l == "flip" || l == "opposite" -> TOWARD_PLAYER
                    l.startsWith("same") || l == "copy" || l == "away" -> SAME_AS_PLAYER
                    l.startsWith("fixed") -> FIXED
                    else -> null
                }
            }
        }
    }

    /** Nearest multiple of [step] degrees in [0, 360); a step of 0 or less leaves the angle unsnapped. */
    @JvmStatic
    fun snap(yaw: Double, step: Double): Double {
        val n = Frame.norm360(yaw)
        if (step <= 0) return n
        return Frame.norm360(Math.round(n / step) * step)
    }

    /** Yaw for a shop/upgrade placed by a player facing [playerYaw]. */
    @JvmStatic
    fun shopYaw(mode: Mode, fixedYaw: Double, playerYaw: Double, step: Double): Double {
        val raw = when (mode) {
            Mode.SAME_AS_PLAYER -> playerYaw
            Mode.FIXED -> fixedYaw
            Mode.TOWARD_PLAYER -> playerYaw + 180.0
        }
        return snap(raw, step)
    }
}
