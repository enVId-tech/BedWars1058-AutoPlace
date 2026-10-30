package dev.etran.tools

import dev.etran.gui.Items
import dev.etran.model.Element
import dev.etran.model.SetupSession
import org.bukkit.ChatColor
import org.bukkit.inventory.ItemStack

/**
 * The hotbar tools. Each is recognised by a hidden marker in the last lore line, so a tool survives being
 * moved around and is never confused with the player's own items.
 */
enum class Tool(
    @JvmField val id: String,
    @JvmField val element: Element?,
    private val icon: String,
    private val toolName: String
) {
    TEAM("team", null, "WHITE_WOOL|WOOL:0", "&e&lTeam"),
    SPAWN("spawn", Element.SPAWN, "ENDER_PEARL", "&b&lTeam Spawn"),
    BED("bed", Element.BED, "RED_BED|BED", "&c&lBed"),
    SHOP("shop", Element.SHOP, "VILLAGER_SPAWN_EGG|MONSTER_EGG:120|EMERALD", "&a&lItem Shop"),
    UPGRADE("upgrade", Element.UPGRADE, "ENCHANTING_TABLE|ENCHANTMENT_TABLE", "&d&lTeam Upgrades"),
    IRON("iron", Element.GEN_IRON, "IRON_INGOT", "&f&lIron Generator"),
    GOLD("gold", Element.GEN_GOLD, "GOLD_INGOT", "&6&lGold Generator"),
    DROPS("drops", Element.KILL_DROPS, "DROPPER", "&7&lDrop Area"),
    MENU("menu", null, "NETHER_STAR", "&6&lMenu"),
    WAIT_SPAWN("waitspawn", Element.WAITING_SPAWN, "BEACON", "&b&lPlayer (Waiting) Spawn"),
    SPEC_SPAWN("specspawn", Element.SPECTATOR_SPAWN, Items.SPECTRAL_ARROW, "&7&lSpectator Spawn"),
    LOBBY1("lobby1", Element.LOBBY_POS1, Items.WOODEN_AXE, "&e&lLobby Corner 1"),
    LOBBY2("lobby2", Element.LOBBY_POS2, "STONE_AXE", "&e&lLobby Corner 2"),
    DIAMOND("diamond", Element.GEN_DIAMOND, "DIAMOND", "&b&lDiamond Generator"),
    EMERALD("emerald", Element.GEN_EMERALD, "EMERALD", "&a&lEmerald Generator"),
    PAGE_TEAM("pageteam", null, "RED_BED|BED", "&e&lBack to team tools");

    /** The item for this tool, personalised with the session's selected team. */
    fun item(s: SetupSession?): ItemStack {
        val team = s?.selected
        var spec = icon
        var title = toolName
        val lore = ArrayList<String>()
        when (this) {
            TEAM -> {
                if (team != null) {
                    spec = team.woolSpec()
                    title = "&e&lTeam: " + team.chat + team.display
                } else {
                    title = "&e&lTeam: &7none selected"
                }
                lore.add("&7Right-click: choose the team you place for,")
                lore.add("&7or create the teams.")
            }
            MENU -> {
                lore.add("&7Right-click: open the full menu")
                lore.add("&7(gamemode, lobby, options, validation).")
                lore.add("&7Left-click: switch between team and map tools.")
            }
            PAGE_TEAM -> lore.add("&7Right-click: show the team tools again.")
            else -> {
                if (this == BED && team != null) spec = team.bedSpec()
                lore.add("&7Right-click a block: place it &fthere&7")
                lore.add("&7(you don't have to stand on it).")
                if (this == SHOP || this == UPGRADE) lore.add("&7It looks back toward you, snapped to 45°.")
                if (this == BED) lore.add("&7Click the bed itself.")
                lore.add("&7Left-click an element to delete it.")
                if (element != null && element.perTeam && team != null) {
                    lore.add("&7Placing for: " + team.chat + team.display)
                }
            }
        }
        lore.add("&0$MARKER$id")
        return Items.of(spec, title, lore)
    }

    companion object {
        @JvmField
        val TEAM_PAGE: Array<Tool?> = arrayOf(TEAM, SPAWN, BED, SHOP, UPGRADE, IRON, GOLD, DROPS, MENU)

        @JvmField
        val MAP_PAGE: Array<Tool?> = arrayOf(WAIT_SPAWN, SPEC_SPAWN, LOBBY1, LOBBY2, DIAMOND, EMERALD, null, PAGE_TEAM, MENU)

        private const val MARKER = "bwauto:"

        /** Which tool an item is, or null for an ordinary item. */
        @JvmStatic
        fun fromItem(item: ItemStack?): Tool? {
            if (item == null || !item.hasItemMeta()) return null
            val m = item.itemMeta ?: return null
            if (!m.hasLore()) return null
            val lore = m.lore
            if (lore.isNullOrEmpty()) return null
            val last = ChatColor.stripColor(lore[lore.size - 1])
            if (last == null || !last.startsWith(MARKER)) return null
            val id = last.substring(MARKER.length)
            return entries.firstOrNull { it.id == id }
        }
    }
}
