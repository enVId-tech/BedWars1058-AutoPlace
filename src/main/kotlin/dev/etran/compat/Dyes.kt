package dev.etran.compat

import org.bukkit.Material

/**
 * Dye colour of a coloured block family on both material systems.
 * 1.13+: the colour is the material name prefix ("LIME_WOOL").
 * 1.8-1.12: one material ("WOOL", "STAINED_CLAY", ...) plus a data value 0-15.
 */
object Dyes {
    /** Index = legacy data value. */
    @JvmField
    val BY_DATA: List<String> = listOf(
        "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
        "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    )

    /** {family key, modern name suffix, legacy material name} */
    private val FAMILIES: Array<Array<String>> = arrayOf(
        arrayOf("WOOL", "_WOOL", "WOOL"),
        arrayOf("CONCRETE", "_CONCRETE", "CONCRETE"),
        arrayOf("TERRACOTTA", "_TERRACOTTA", "STAINED_CLAY"),
        arrayOf("GLASS", "_STAINED_GLASS", "STAINED_GLASS"),
        arrayOf("CARPET", "_CARPET", "CARPET")
    )

    @JvmStatic
    fun dataOf(dye: String): Int = maxOf(0, BY_DATA.indexOf(dye))

    /** True if the legacy material could belong to one of the families (so reading its data is worthwhile). */
    @JvmStatic
    fun isLegacyFamily(m: Material, families: List<String>): Boolean {
        val n = m.name
        return FAMILIES.any { families.contains(it[0]) && it[2] == n }
    }

    /**
     * @param legacyData the block's data value; only read when [Compat.FLATTENED] is false
     * @return the dye name ("LIME", "RED", ...) or null if the block is not in one of the families
     */
    @JvmStatic
    fun dyeOf(m: Material, legacyData: Int, families: List<String>): String? {
        val n = m.name
        for (f in FAMILIES) {
            if (!families.contains(f[0])) continue
            if (Compat.FLATTENED) {
                if (n.endsWith(f[1])) {
                    val dye = n.substring(0, n.length - f[1].length)
                    if (BY_DATA.contains(dye)) return dye
                }
            } else if (n == f[2]) {
                return BY_DATA[legacyData and 15]
            }
        }
        return null
    }
}
