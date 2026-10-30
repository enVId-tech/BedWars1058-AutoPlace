package dev.etran.gui

import dev.etran.EasySetupPlugin
import dev.etran.Msg
import dev.etran.bw.ActionQueue
import dev.etran.bw.ElementPlacer
import dev.etran.model.Element
import dev.etran.place.AutoRunner
import dev.etran.place.NextGen
import org.bukkit.entity.Player

class MapGenMenu(plugin: EasySetupPlugin, player: Player) : Menu(plugin, player) {
    override fun title(): String = "Map Generators"

    override fun rows(): Int = 3

    private fun gen(slot: Int, icon: String, e: Element) {
        val n = session.mapCount(e)
        set(
            slot,
            Items.of(icon, (if (n > 0) "&a✔ " else "&c✘ ") + e.label, "&7Placed: &f$n", "&eClick to place at your position")
        ) { ElementPlacer.placeHere(plugin, player, session, null, e) { refresh() } }
    }

    override fun render() {
        fill(Items.GRAY_PANE)
        gen(10, "DIAMOND", Element.GEN_DIAMOND)
        gen(11, "EMERALD", Element.GEN_EMERALD)
        set(
            13,
            Items.of(
                "DIAMOND_BLOCK", "&bAuto-place all generators",
                "&7Scores every diamond/emerald block for how",
                "&7generator-like it is (headroom, pedestal,",
                "&7map symmetry) and places the likely ones.",
                "&7See /bwauto gens for the probabilities."
            )
        ) { placeAll() }
        val deg = plugin.settings().defaultRotation()
        set(
            15,
            Items.of(
                Items.CLOCK, "&6Rotate around map centre (" + deg.toLong() + "°)",
                "&7Moves you to the same spot on the next",
                "&7island. Use &f/nextgen <degrees>&7 for any angle,",
                "&7or &f/nextgen diamond|emerald [degrees]&7 to",
                "&7detect and place one at a time."
            )
        ) { NextGen.rotate(plugin, player, deg) }
        set(18, Items.of(Items.RED_CONCRETE, "&c&lBack")) { go(MainMenu(plugin, player)) }
    }

    private fun placeAll() {
        if (session.busy) {
            Msg.err(player, "Another placement is still running - wait a moment.")
            return
        }
        val back = player.location.clone()
        val q = ActionQueue(plugin, player)
        val n = AutoRunner.queueGenerators(plugin, player, session, q, player.world, back)
        if (n[0] + n[1] == 0) return
        player.closeInventory()
        session.busy = true
        q.teleport(back)
        q.done {
            session.busy = false
            Msg.ok(player, "Placed ${n[0]} diamond and ${n[1]} emerald generator(s).")
        }
        q.start()
    }
}
