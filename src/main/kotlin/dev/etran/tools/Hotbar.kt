package dev.etran.tools

import dev.etran.EasySetupPlugin
import dev.etran.Msg
import dev.etran.model.SetupSession
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.io.IOException

/**
 * Setup mode puts the tools on the player's hotbar. The items they replace are kept in memory and on disk
 * (`saved-hotbars/<uuid>.yml`), so they come back on exit, quit, crash or restart.
 */
@Suppress("DEPRECATION")
object Hotbar {
    private fun file(plugin: EasySetupPlugin, p: Player): File =
        File(File(plugin.dataFolder, "saved-hotbars"), p.uniqueId.toString() + ".yml")

    @JvmStatic
    fun isTool(item: ItemStack?): Boolean = Tool.fromItem(item) != null

    /** Start setup mode: load what already exists in the arena file, then put the tools on the hotbar. */
    @JvmStatic
    fun enter(plugin: EasySetupPlugin, p: Player, s: SetupSession) {
        if (plugin.settings().importExisting() && !s.imported) {
            val n = ArenaImporter.importInto(plugin, p.world, s)
            if (n > 0) Msg.info(p, "Loaded $n existing element(s) from this arena's Bedwars1058 file.")
        }
        activate(plugin, p, s)
    }

    @JvmStatic
    fun activate(plugin: EasySetupPlugin, p: Player, s: SetupSession) {
        if (s.toolsActive) {
            render(plugin, p, s)
            return
        }
        val inv = p.inventory
        val saved = arrayOfNulls<ItemStack>(9)
        val cfg = YamlConfiguration()
        for (i in 0 until 9) {
            var it = inv.getItem(i)
            if (isTool(it)) it = null // never save a tool as the player's own item
            saved[i] = it?.clone()
            if (it != null) cfg.set("slot$i", it)
        }
        s.savedHotbar = saved
        s.savedHeldSlot = inv.heldItemSlot
        cfg.set("held", s.savedHeldSlot)
        try {
            val f = file(plugin, p)
            f.parentFile.mkdirs()
            cfg.save(f)
        } catch (ex: IOException) {
            plugin.logger.warning("Could not save ${p.name}'s hotbar to disk: ${ex.message}")
        }
        s.toolsActive = true
        render(plugin, p, s)
        plugin.ghosts().refresh(p, s)
        Msg.ok(
            p,
            "Setup tools are on your hotbar. Right-click a block to place, left-click an element to delete. " +
                "The nether star opens the menu; /bwsetup exit gives your items back."
        )
    }

    @JvmStatic
    fun deactivate(plugin: EasySetupPlugin, p: Player, s: SetupSession) {
        if (!s.toolsActive) return
        s.toolsActive = false
        plugin.ghosts().removeOwner(p.uniqueId)
        val inv = p.inventory
        val saved = s.savedHotbar
        for (i in 0 until 9) inv.setItem(i, saved?.get(i))
        stripTools(p)
        inv.heldItemSlot = s.savedHeldSlot.coerceIn(0, 8)
        s.savedHotbar = null
        file(plugin, p).delete()
        p.updateInventory()
    }

    /** Draw the current page of tools onto the hotbar. */
    @JvmStatic
    fun render(plugin: EasySetupPlugin, p: Player, s: SetupSession) {
        if (!s.toolsActive) return
        val page = if (s.hotbarPage == 1) Tool.MAP_PAGE else Tool.TEAM_PAGE
        val inv = p.inventory
        for (i in 0 until 9) inv.setItem(i, page[i]?.item(s))
        p.updateInventory()
    }

    @JvmStatic
    fun setPage(plugin: EasySetupPlugin, p: Player, s: SetupSession, page: Int) {
        s.hotbarPage = if (page == 1) 1 else 0
        render(plugin, p, s)
        Msg.info(
            p,
            if (s.hotbarPage == 1) "Map tools: spawns, lobby corners, diamond and emerald generators."
            else "Team tools: spawn, bed, shop, upgrade, generators, drop area."
        )
    }

    @JvmStatic
    fun stripTools(p: Player) {
        val inv = p.inventory
        val c = inv.contents
        for (i in c.indices) if (isTool(c[i])) inv.setItem(i, null)
    }

    /** On join: if tools were left behind by a crash or restart, put the player's own items back. */
    @JvmStatic
    fun restoreLeftovers(plugin: EasySetupPlugin, p: Player) {
        val f = file(plugin, p)
        val hadFile = f.exists()
        if (hadFile) {
            val cfg = YamlConfiguration.loadConfiguration(f)
            val inv = p.inventory
            for (i in 0 until 9) {
                if (!cfg.contains("slot$i")) {
                    if (isTool(inv.getItem(i))) inv.setItem(i, null)
                    continue
                }
                val o = cfg.get("slot$i")
                if (o is ItemStack) inv.setItem(i, o)
            }
            f.delete()
        }
        stripTools(p)
        p.updateInventory()
        if (hadFile) Msg.info(p, "Your hotbar items were restored after an interrupted setup session.")
    }
}
