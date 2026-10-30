package dev.etran.model

import java.util.Locale

/**
 * Reads the Bedwars1058 arena file layout tolerantly. The exact keys differ between versions, so instead of
 * hard-coding paths this recognises a location by its value ("x,y,z[,yaw,pitch]") and the element by words in
 * its key path (Team.Red.Shop, ...Upgrade, ...Spawn, ...Bed, Iron/Gold, Diamond/Emerald, waiting Pos1/Pos2 ...).
 */
object ArenaFormat {
    class Slot internal constructor(
        /** Team name from the path, or null for map-wide keys. */
        @JvmField val team: String?,
        @JvmField val element: Element
    )

    /** {x, y, z, yaw, pitch}, or null when the text is not a location. A leading world name is ignored. */
    @JvmStatic
    fun parseLocation(s: String?): DoubleArray? {
        if (s == null) return null
        val parts = s.trim().split(Regex("\\s*,\\s*"))
        val nums = DoubleArray(parts.size)
        var n = 0
        for (i in parts.indices) {
            val d = parts[i].toDoubleOrNull()
            if (d == null) {
                if (i == 0) continue // world name
                return null
            }
            nums[n++] = d
        }
        if (n < 3) return null
        return doubleArrayOf(nums[0], nums[1], nums[2], if (n > 3) nums[3] else 0.0, if (n > 4) nums[4] else 0.0)
    }

    /** What a key path refers to, or null when it is not an element we track. */
    @JvmStatic
    fun classify(path: String?): Slot? {
        if (path == null) return null
        var team: String? = null
        val rest = StringBuilder()
        for (seg in path.split(Regex("\\."))) {
            val c = TeamColor.fromName(seg)
            if (c != null && team == null) team = c.name
            else rest.append(seg.lowercase(Locale.ROOT).replace("_", "").replace("-", "")).append('.')
        }
        val r = rest.toString()
        val t = team
        val e: Element? = when {
            r.contains("pos1") -> if (t == null) Element.LOBBY_POS1 else null
            r.contains("pos2") -> if (t == null) Element.LOBBY_POS2 else null
            r.contains("shop") -> if (t == null) null else Element.SHOP
            r.contains("upgrade") -> if (t == null) null else Element.UPGRADE
            r.contains("killdrop") || r.contains("drops") -> if (t == null) null else Element.KILL_DROPS
            r.contains("bed") -> if (t == null) null else Element.BED
            r.contains("iron") -> if (t == null) null else Element.GEN_IRON
            r.contains("gold") -> if (t == null) null else Element.GEN_GOLD
            r.contains("diamond") -> if (t == null) Element.GEN_DIAMOND else null
            r.contains("emerald") -> if (t == null) Element.GEN_EMERALD else null
            r.contains("spectat") -> if (t == null) Element.SPECTATOR_SPAWN else null
            r.contains("spawn") -> if (t == null) Element.WAITING_SPAWN else Element.SPAWN
            r.contains("waiting") || r.contains("lobby") -> if (t == null) Element.WAITING_SPAWN else null
            else -> null
        }
        return if (e == null) null else Slot(t, e)
    }
}
