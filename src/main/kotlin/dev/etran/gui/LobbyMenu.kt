package dev.etran.gui

import dev.etran.EasySetupPlugin
import dev.etran.Msg
import dev.etran.bw.ActionQueue
import dev.etran.bw.ElementPlacer
import dev.etran.model.Element
import dev.etran.place.AutoRunner
import org.bukkit.entity.Player

class LobbyMenu(plugin: EasySetupPlugin, player: Player) : Menu(plugin, player) {
    override fun title(): String = "Lobby & Boundary"

    override fun rows(): Int = 3

    private fun marker(slot: Int, icon: String, e: Element, hint: String) {
        val placed = session.mapPlaced(e)
        set(
            slot,
            Items.of(icon, (if (placed) "&a✔ " else "&c✘ ") + e.label, "&7$hint", "&eClick to place at your position")
        ) { ElementPlacer.placeHere(plugin, player, session, null, e) { refresh() } }
    }

    override fun render() {
        fill(Items.GRAY_PANE)
        marker(10, "ENDER_PEARL", Element.WAITING_SPAWN, "Where players wait before the game.")
        marker(11, Items.SPECTRAL_ARROW, Element.SPECTATOR_SPAWN, "Where spectators appear.")
        marker(12, Items.WOODEN_AXE, Element.LOBBY_POS1, "First corner of the region removed at start.")
        marker(13, "STONE_AXE", Element.LOBBY_POS2, "Opposite corner of that region.")
        set(
            15,
            Items.of(
                Items.REPEATER, "&6Boundary: " + (if (session.autoLobby) "&aAutomatic" else "&eManual"),
                "&7Semi-Automatic mode: should automatic",
                "&7placement detect the lobby for you?",
                "&8Fully Automatic mode always detects it.",
                "&eClick to toggle"
            )
        ) {
            session.autoLobby = !session.autoLobby
            refresh()
        }
        set(
            16,
            Items.of(
                Items.ENDER_EYE, "&dDetect lobby structure now",
                "&7Scans around the map centre, builds a",
                "&7bounding box and puts both spawns at its",
                "&7exact centre."
            )
        ) { detectNow() }
        set(18, Items.of(Items.RED_CONCRETE, "&c&lBack")) { go(MainMenu(plugin, player)) }
        set(25, Items.of(Items.LIME_CONCRETE, "&a&lTeam & Base Setup ➜")) {
            if (session.gamemode == null) {
                Msg.err(player, "Pick a gamemode first.")
            } else {
                if (session.selected == null) session.selected = session.requiredTeams()[0]
                go(BaseMenu(plugin, player))
            }
        }
    }

    private fun detectNow() {
        if (session.busy) {
            Msg.err(player, "Another placement is still running - wait a moment.")
            return
        }
        val q = ActionQueue(plugin, player)
        val back = player.location.clone()
        if (!AutoRunner.queueLobbyDetection(plugin, player, session, q, player.world)) return
        player.closeInventory()
        session.busy = true
        q.teleport(back)
        q.done {
            session.busy = false
            Msg.ok(player, "Lobby spawns and region placed.")
        }
        q.start()
    }
}
