package dev.etran.gui

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Item helpers. Item specs are `NAME|LEGACY:data|...`: the first material that exists on the running server
 * wins, which is how one jar builds the same menu on 1.8 (WOOL:14) and 1.13+ (RED_WOOL).
 */
object Items {
    const val RED_CONCRETE = "RED_CONCRETE|CONCRETE:14|STAINED_CLAY:14"
    const val LIME_CONCRETE = "LIME_CONCRETE|CONCRETE:5|STAINED_CLAY:5"
    const val GRAY_PANE = "GRAY_STAINED_GLASS_PANE|STAINED_GLASS_PANE:7"
    const val LIME_PANE = "LIME_STAINED_GLASS_PANE|STAINED_GLASS_PANE:5"
    const val RED_PANE = "RED_STAINED_GLASS_PANE|STAINED_GLASS_PANE:14"
    const val LIME_DYE = "LIME_DYE|INK_SACK:10"
    const val RED_DYE = "RED_DYE|INK_SACK:1"
    const val ENDER_EYE = "ENDER_EYE|EYE_OF_ENDER"
    const val COMPARATOR = "COMPARATOR|REDSTONE_COMPARATOR"
    const val CLOCK = "CLOCK|WATCH"
    const val REPEATER = "REPEATER|DIODE"
    const val WOODEN_AXE = "WOODEN_AXE|WOOD_AXE"
    const val SPECTRAL_ARROW = "SPECTRAL_ARROW|ARROW"
    const val WHITE_WOOL = "WHITE_WOOL|WOOL:0"

    @JvmStatic
    fun color(s: String): String = ChatColor.translateAlternateColorCodes('&', s)

    @JvmStatic
    fun of(spec: String, name: String, vararg lore: String): ItemStack = of(spec, name, lore.toList())

    @JvmStatic
    fun of(spec: String, name: String, lore: List<String>): ItemStack {
        val item = create(spec)
        val meta = item.itemMeta
        if (meta != null) {
            meta.displayName = color(name)
            if (lore.isNotEmpty()) meta.lore = lore.map { color(it) }
            item.itemMeta = meta
        }
        return item
    }

    /** Resolve a spec to an item; falls back to paper if no candidate exists on this server. */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun create(spec: String): ItemStack {
        for (cand in spec.split("|")) {
            var name = cand
            var data = 0
            val colon = cand.indexOf(':')
            if (colon >= 0) {
                name = cand.substring(0, colon)
                data = cand.substring(colon + 1).toIntOrNull() ?: 0
            }
            val m = Material.matchMaterial(name)
            if (m != null) return if (data == 0) ItemStack(m) else ItemStack(m, 1, data.toShort())
        }
        return ItemStack(Material.PAPER)
    }
}
