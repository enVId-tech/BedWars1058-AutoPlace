package dev.etran.gui

import dev.etran.EasySetupPlugin
import dev.etran.Msg
import dev.etran.model.Element
import dev.etran.place.TeamManager
import dev.etran.place.Validator
import dev.etran.tools.Hotbar
import org.bukkit.ChatColor
import org.bukkit.entity.Player

/** Final screen: green/red per element; the save button needs a second click while anything is missing. */
class ValidationMenu(plugin: EasySetupPlugin, player: Player) : Menu(plugin, player) {
    override fun title(): String = "Final Validation"

    override fun rows(): Int = 6

    override fun render() {
        fill(Items.GRAY_PANE)
        val missing = Validator.missingAll(session, plugin.settings())
        val complete = missing.isEmpty()
        set(
            4,
            Items.of(
                if (complete) Items.LIME_DYE else Items.RED_DYE,
                if (complete) "&a&lEverything is placed" else "&c&lSomething is missing",
                if (complete) listOf("&7Ready to save.")
                else listOf("&7Click any red item to place it,", "&7or save anyway with the green button.")
            )
        )

        // Teams, one row each (rows 1-4).
        val req = session.requiredTeams()
        for (i in 0 until minOf(req.size, 4)) {
            val c = req[i]
            val row = 9 * (i + 1)
            val exists = session.hasTeam(c)
            set(
                row,
                Items.of(c.woolSpec(), c.chat.toString() + c.display, if (exists) "&7Team exists" else "&cTeam not created - click to create")
            ) {
                if (!session.hasTeam(c)) {
                    TeamManager.autoCreate(plugin, player, session) { refresh() }
                } else {
                    session.selected = c
                    go(BaseMenu(plugin, player))
                }
            }
            val te = Element.teamElements()
            for (k in 0 until minOf(te.size, 7)) {
                val el = te[k]
                val ok = session.teamPlaced(c, el)
                set(
                    row + 1 + k,
                    Items.of(
                        if (ok) Items.LIME_PANE else Items.RED_PANE,
                        (if (ok) "&a✔ " else "&c✘ ") + el.label,
                        if (ok) "&7Placed (click to edit)" else "&eMissing - click to place"
                    )
                ) {
                    if (!session.hasTeam(c)) {
                        TeamManager.requireTeam(player, session, c)
                    } else {
                        session.selected = c
                        go(BaseMenu(plugin, player))
                    }
                }
            }
        }

        // Map elements (bottom row).
        val me = Element.mapElements()
        for (k in 0 until minOf(me.size, 6)) {
            val el = me[k]
            val ok = session.mapPlaced(el)
            set(
                46 + k,
                Items.of(
                    if (ok) Items.LIME_PANE else Items.RED_PANE,
                    (if (ok) "&a✔ " else "&c✘ ") + el.label,
                    if (ok) "&7Placed (click to edit)" else "&eMissing - click to place"
                )
            ) { go(if (el.isGenerator()) MapGenMenu(plugin, player) else LobbyMenu(plugin, player)) }
        }

        set(45, Items.of(Items.RED_CONCRETE, "&c&lBack")) { go(BaseMenu(plugin, player)) }
        val armed = !complete && System.currentTimeMillis() < session.saveConfirmUntil
        val name = if (complete) "&a&lConfirm & Save" else if (armed) "&a&lClick again to save anyway" else "&a&lSave anyway..."
        set(
            53,
            Items.of(
                Items.LIME_CONCRETE, name,
                if (complete) listOf("&7Runs the Bedwars save command.")
                else listOf("&cThe arena is incomplete.", "&7First click lists what is missing.")
            )
        ) { confirm() }
    }

    private fun confirm() {
        val missing = Validator.missingAll(session, plugin.settings())
        if (missing.isEmpty()) {
            save()
            return
        }
        if (System.currentTimeMillis() < session.saveConfirmUntil) {
            save()
            return
        }
        session.saveConfirmUntil = System.currentTimeMillis() + CONFIRM_WINDOW_MS
        player.sendMessage(Msg.PREFIX + ChatColor.RED + "The arena is incomplete. Missing:")
        for (line in missing) player.sendMessage(ChatColor.GRAY.toString() + " - " + ChatColor.WHITE + line)
        player.sendMessage(
            Msg.PREFIX + ChatColor.YELLOW + "Click the red items to place them, the red concrete to go " +
                "back, or click the green concrete again within 10 seconds to save anyway."
        )
        refresh()
    }

    private fun save() {
        player.closeInventory()
        val cmd = plugin.bw().build("save")
        if (cmd == null) {
            Msg.err(player, "No save command is configured (bw-commands.save in config.yml).")
            return
        }
        // Hotbar tools and ghost villagers go first, so none of it can end up in the saved arena.
        Hotbar.deactivate(plugin, player, session)
        plugin.ghosts().removeOwner(player.uniqueId)
        player.performCommand(cmd)
        Msg.ok(player, "Save command sent: /$cmd")
        plugin.sessions().remove(player.uniqueId)
    }

    private companion object {
        const val CONFIRM_WINDOW_MS = 10_000L
    }
}
