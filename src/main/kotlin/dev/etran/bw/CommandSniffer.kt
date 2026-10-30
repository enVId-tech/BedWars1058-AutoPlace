package dev.etran.bw

import dev.etran.EasySetupPlugin
import dev.etran.model.Element
import dev.etran.model.SetupSession
import dev.etran.model.TeamColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.Locale

/**
 * Watches the Bedwars1058 setup commands a player types by hand and records them in the session, so manual
 * and menu-driven setup stay in sync (the Skript's `on command`).
 */
class CommandSniffer(private val plugin: EasySetupPlugin) : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCommand(ev: PlayerCommandPreprocessEvent) {
        val p = ev.player
        val s = plugin.sessions().peek(p) ?: return
        var msg = ev.message.trim()
        if (msg.startsWith("/")) msg = msg.substring(1)
        val a = msg.split(Regex("\\s+"))
        if (a.size < 2 || !plugin.settings().commandRoots().contains(a[0].lowercase(Locale.ROOT))) return
        when (a[1].lowercase(Locale.ROOT)) {
            "createteam" -> {
                val c = if (a.size > 2) TeamColor.fromName(a[2]) else null
                if (c != null) s.team(c).created = true
            }
            "removeteam" -> {
                val c = if (a.size > 2) TeamColor.fromName(a[2]) else null
                if (c != null) {
                    val key = s.teams.remove(c)?.bed?.key()
                    if (key != null) s.assignedBeds.removeAll { it.key() == key }
                }
            }
            "setspawn" -> teamElement(p, s, a, 2, Element.SPAWN)
            "setbed" -> teamElement(p, s, a, 2, Element.BED)
            "setshop" -> teamElement(p, s, a, 2, Element.SHOP)
            "setupgrade" -> teamElement(p, s, a, 2, Element.UPGRADE)
            "setkilldrops" -> teamElement(p, s, a, 2, Element.KILL_DROPS)
            "addgenerator" -> {
                if (a.size < 3) return
                when (a[2].lowercase(Locale.ROOT)) {
                    "iron" -> teamElement(p, s, a, 3, Element.GEN_IRON)
                    "gold" -> teamElement(p, s, a, 3, Element.GEN_GOLD)
                    "diamond" -> ElementPlacer.record(plugin, p, s, null, Element.GEN_DIAMOND, p.location)
                    "emerald" -> ElementPlacer.record(plugin, p, s, null, Element.GEN_EMERALD, p.location)
                }
            }
            "setwaitingspawn" -> ElementPlacer.record(plugin, p, s, null, Element.WAITING_SPAWN, p.location)
            "setspectatorspawn" -> ElementPlacer.record(plugin, p, s, null, Element.SPECTATOR_SPAWN, p.location)
            "waitingpos1" -> ElementPlacer.record(plugin, p, s, null, Element.LOBBY_POS1, p.location)
            "waitingpos2" -> ElementPlacer.record(plugin, p, s, null, Element.LOBBY_POS2, p.location)
        }
        if (s.toolsActive) {
            Bukkit.getScheduler().runTask(plugin, Runnable { plugin.ghosts().refresh(p, s) })
        }
    }

    private fun teamElement(p: Player, s: SetupSession, a: List<String>, teamArg: Int, e: Element) {
        val c = (if (a.size > teamArg) TeamColor.fromName(a[teamArg]) else s.selected) ?: return
        ElementPlacer.record(plugin, p, s, c, e, p.location)
    }

    @EventHandler
    fun onQuit(ev: PlayerQuitEvent) {
        plugin.sessions().remove(ev.player.uniqueId)
    }
}
