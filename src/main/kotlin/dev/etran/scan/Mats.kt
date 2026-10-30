package dev.etran.scan

import dev.etran.compat.Dyes
import dev.etran.infer.Kind
import org.bukkit.Material
import org.bukkit.block.Block

/** Material helpers that work off names so they behave the same on legacy and flattened versions. */
object Mats {
    /** BED_BLOCK on 1.8-1.12, RED_BED etc. on 1.13+. */
    @JvmStatic
    fun isBed(m: Material): Boolean {
        val n = m.name
        return n == "BED_BLOCK" || n.endsWith("_BED")
    }

    @JvmStatic
    fun isStairs(m: Material): Boolean = m.name.endsWith("STAIRS")

    /** AIR, plus CAVE_AIR / VOID_AIR on 1.13+. */
    @JvmStatic
    fun isAir(m: Material): Boolean = m == Material.AIR || m.name.endsWith("_AIR")

    private val KIND_CACHE = arrayOfNulls<Kind>(Material.values().size)

    /** Coarse class of a material for the detection algorithms; works on legacy and flattened names. */
    @JvmStatic
    fun kind(m: Material): Kind {
        var k = KIND_CACHE[m.ordinal]
        if (k == null) {
            k = classify(m)
            KIND_CACHE[m.ordinal] = k
        }
        return k
    }

    private fun classify(m: Material): Kind {
        if (isAir(m)) return Kind.AIR
        val n = m.name
        if (n == "IRON_BLOCK") return Kind.IRON_BLOCK
        if (n == "GOLD_BLOCK") return Kind.GOLD_BLOCK
        if (n == "DIAMOND_BLOCK") return Kind.DIAMOND_BLOCK
        if (n == "EMERALD_BLOCK") return Kind.EMERALD_BLOCK
        if (isBed(m)) return Kind.BED
        if (isStairs(m)) return Kind.STAIRS
        if (n.endsWith("_SLAB") || n == "STEP" || n == "WOOD_STEP" || n == "STONE_SLAB2") return Kind.SLAB
        return if (m.isSolid) Kind.SOLID else Kind.PASSABLE
    }

    /** A block an entity can stand inside. */
    @JvmStatic
    fun passable(m: Material): Boolean = !m.isSolid

    @JvmStatic
    fun passable(b: Block): Boolean = passable(b.type)

    /**
     * Material name without a leading dye colour, so "RED_WOOL" and "BLUE_WOOL" compare equal. Legacy
     * names carry no colour already; applying the same rule on both sides keeps comparisons consistent.
     */
    @JvmStatic
    fun family(m: Material): String {
        val n = m.name
        for (d in Dyes.BY_DATA) {
            if (n.startsWith(d + "_")) return n.substring(d.length + 1)
        }
        return n
    }
}
