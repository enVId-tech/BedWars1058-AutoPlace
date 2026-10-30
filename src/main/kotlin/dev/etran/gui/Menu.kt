package dev.etran.gui

import dev.etran.EasySetupPlugin
import dev.etran.model.SetupSession
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/** Base class for the chest menus: slot -> click action, re-renderable. */
abstract class Menu protected constructor(
    protected val plugin: EasySetupPlugin,
    protected val player: Player
) : InventoryHolder {
    protected val session: SetupSession = plugin.sessions().get(player)
    private var inv: Inventory? = null
    private var closed = true
    private val actions = HashMap<Int, (InventoryClickEvent) -> Unit>()

    protected abstract fun title(): String

    protected abstract fun rows(): Int

    protected abstract fun render()

    fun open() {
        val i = Bukkit.createInventory(this, rows() * 9, title())
        inv = i
        closed = false
        render()
        player.openInventory(i)
    }

    /** Re-draw in place. Does nothing if the player has since closed or left this menu. */
    fun refresh() {
        val i = inv
        if (i == null || closed) return
        i.clear()
        actions.clear()
        render()
    }

    internal fun markClosed() {
        closed = true
    }

    internal fun set(slot: Int, item: ItemStack, action: ((InventoryClickEvent) -> Unit)? = null) {
        inv!!.setItem(slot, item)
        if (action != null) actions[slot] = action
    }

    protected fun fill(spec: String) {
        val i = inv!!
        val filler = Items.of(spec, " ")
        for (n in 0 until i.size) i.setItem(n, filler)
    }

    internal fun handle(e: InventoryClickEvent) {
        actions[e.rawSlot]?.invoke(e)
    }

    /** Open another menu one tick later (never swap inventories from inside a click event). */
    protected fun go(next: Menu) {
        Bukkit.getScheduler().runTask(plugin, Runnable { next.open() })
    }

    override fun getInventory(): Inventory = inv!!
}
