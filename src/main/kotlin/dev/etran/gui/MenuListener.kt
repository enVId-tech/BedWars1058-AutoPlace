package dev.etran.gui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

/** Routes clicks in plugin menus to the menu and stops items from being taken out. */
class MenuListener : Listener {
    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        val top = e.inventory
        val menu = top.holder as? Menu ?: return
        e.isCancelled = true
        if (e.whoClicked !is Player) return
        // Only clicks inside the menu itself (not the player's own inventory below it).
        if (e.rawSlot < 0 || e.rawSlot >= top.size) return
        menu.handle(e)
    }

    @EventHandler
    fun onDrag(e: InventoryDragEvent) {
        if (e.inventory.holder is Menu) e.isCancelled = true
    }

    @EventHandler
    fun onClose(e: InventoryCloseEvent) {
        (e.inventory.holder as? Menu)?.markClosed()
    }
}
