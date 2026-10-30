package dev.etran.tools

import dev.etran.EasySetupPlugin
import dev.etran.Msg
import dev.etran.bw.ActionQueue
import dev.etran.model.Element
import dev.etran.model.SetupSession
import dev.etran.model.TeamColor
import dev.etran.model.TeamState
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * Deleting recorded elements by hitting them. Finding what is at a spot, the two-hit confirmation, running the
 * Bedwars1058 remove command (when one is configured) and forgetting the element plugin-side.
 */
object Remover {
    class Target internal constructor(
        @JvmField val element: Element,
        @JvmField val team: TeamColor?,
        @JvmField val loc: Location,
        /** Team generator record, else null. */
        @JvmField val gen: TeamState.GenRecord?,
        @JvmField val mapGen: Boolean
    ) {
        fun name(): String = (if (team == null) "" else team.display + " ") + element.label

        internal fun key(): String = name() + "@" + loc.blockX + "," + loc.blockY + "," + loc.blockZ
    }

    /** Recorded elements within about a block of [center]; optionally only of one element type. */
    @JvmStatic
    fun find(s: SetupSession, center: Location, only: Element?): List<Target> {
        val out = ArrayList<Target>()
        for ((color, ts) in s.teams) {
            for ((e, loc) in ts.pos) {
                if (only != null && e != only) continue
                if (near(loc, center)) out.add(Target(e, color, loc, null, false))
            }
            for (g in ts.gens) {
                val ge = if (g.type == "iron") Element.GEN_IRON else Element.GEN_GOLD
                if (only != null && ge != only) continue
                if (near(g.loc, center)) out.add(Target(ge, color, g.loc, g, false))
            }
        }
        for ((e, locs) in s.mapElements) {
            if (only != null && e != only) continue
            for (l in locs) {
                if (near(l, center)) out.add(Target(e, null, l, null, e.isGenerator()))
            }
        }
        return out
    }

    /** Every recorded element, for the delete commands. */
    @JvmStatic
    fun all(s: SetupSession): List<Target> {
        val out = ArrayList<Target>()
        for ((color, ts) in s.teams) {
            for ((e, loc) in ts.pos) out.add(Target(e, color, loc, null, false))
            for (g in ts.gens) out.add(Target(if (g.type == "iron") Element.GEN_IRON else Element.GEN_GOLD, color, g.loc, g, false))
        }
        for ((e, locs) in s.mapElements) for (l in locs) out.add(Target(e, null, l, null, e.isGenerator()))
        return out
    }

    /**
     * The recorded copies of [e] to delete: for per-team elements the given [team]'s; for map-wide generators the
     * one nearest [near], or all of them with [every].
     */
    @JvmStatic
    fun select(s: SetupSession, e: Element, team: TeamColor?, near: Location?, every: Boolean): List<Target> {
        var list = all(s).filter { it.element == e && (!e.perTeam || it.team == team) }
        if (!every && e.isGenerator() && !e.perTeam && near != null && list.size > 1) {
            list = listOfNotNull(list.filter { it.loc.world == near.world }.minByOrNull { it.loc.distanceSquared(near) })
        }
        return list
    }

    private fun near(a: Location, center: Location): Boolean {
        if (a.world == null || a.world != center.world) return false
        val dx = a.x - center.x
        val dz = a.z - center.z
        val dy = a.y - center.y
        return dx * dx + dz * dz <= 1.0 && dy >= -1.5 && dy <= 1.5
    }

    /**
     * A hit on one or more targets. Entity-backed things (ghost villagers, generator holograms) pass
     * `immediate = true` and are deleted at once; block-located elements need a second hit within the
     * configured window.
     */
    @JvmStatic
    fun hit(plugin: EasySetupPlugin, p: Player, s: SetupSession, targets: List<Target>, immediate: Boolean, prefer: Element?) {
        if (targets.isEmpty()) return
        var chosen: List<Target> = if (prefer != null) targets.filter { it.element == prefer } else emptyList()
        if (chosen.isEmpty()) chosen = targets
        val key = StringBuilder()
        val names = StringBuilder()
        for (t in chosen) {
            key.append(t.key()).append(';')
            if (names.isNotEmpty()) names.append(", ")
            names.append(t.name())
        }
        val now = System.currentTimeMillis()
        if (immediate || (key.toString() == s.pendingDeleteKey && now < s.pendingDeleteUntil)) {
            s.pendingDeleteKey = null
            delete(plugin, p, s, chosen)
            return
        }
        s.pendingDeleteKey = key.toString()
        s.pendingDeleteUntil = now + (plugin.settings().deleteConfirmSeconds() * 1000).toLong()
        Msg.info(p, "Hit it again within ${plugin.settings().deleteConfirmSeconds().toInt()} seconds to delete: $names")
    }

    private fun removeKey(e: Element): String = when (e) {
        Element.SPAWN -> "remove-spawn"
        Element.BED -> "remove-bed"
        Element.SHOP -> "remove-shop"
        Element.UPGRADE -> "remove-upgrade"
        Element.KILL_DROPS -> "remove-kill-drops"
        Element.WAITING_SPAWN -> "remove-waiting-spawn"
        Element.SPECTATOR_SPAWN -> "remove-spectator-spawn"
        Element.LOBBY_POS1 -> "remove-waiting-pos1"
        Element.LOBBY_POS2 -> "remove-waiting-pos2"
        else -> "remove-generator"
    }

    /** Delete targets right now: run the remove command where configured, then forget them. */
    @JvmStatic
    fun delete(plugin: EasySetupPlugin, p: Player, s: SetupSession, targets: List<Target>) {
        if (s.busy) {
            Msg.err(p, "Another placement is still running - wait a moment.")
            return
        }
        val original = p.location.clone()
        val q = ActionQueue(plugin, p)
        val names = StringBuilder()
        var unsynced = false
        for (t in targets) {
            if (names.isNotEmpty()) names.append(", ")
            names.append(t.name())
            val cmd = plugin.bw().build(removeKey(t.element), "team", t.team?.bwName() ?: "")
            if (cmd != null) {
                q.teleport(t.loc)
                q.command(cmd)
            } else {
                unsynced = true
            }
            q.then { forget(s, t) }
        }
        s.busy = true
        q.teleport(original)
        val warn = unsynced
        q.done {
            s.busy = false
            plugin.ghosts().refresh(p, s)
            Msg.ok(p, "Deleted: $names")
            if (warn) {
                Msg.info(
                    p,
                    "Bedwars1058 has no remove command configured for that element (bw-commands.remove-* in " +
                        "config.yml), so its old value stays until you place it again."
                )
            }
        }
        q.start()
    }

    /** Drop an element from the session (and from the template if it was the template base's). */
    @JvmStatic
    fun forget(s: SetupSession, t: Target) {
        if (t.team != null) {
            val ts = s.teams[t.team] ?: return
            if (t.gen != null) {
                ts.gens.remove(t.gen)
                val any = ts.gens.any { it.type == t.gen.type }
                if (!any) {
                    ts.placed.remove(t.element)
                    if (s.template.team == t.team) s.template.remove(t.element)
                }
                return
            }
            ts.pos.remove(t.element)
            ts.placed.remove(t.element)
            if (s.template.team == t.team) s.template.remove(t.element)
            if (t.element == Element.BED) {
                val old = ts.bed
                ts.bed = null
                if (old != null) {
                    val key = old.key()
                    s.assignedBeds.removeAll { it.key() == key }
                    if (s.template.bed?.key() == key) s.template.bed = null
                }
            }
            return
        }
        s.mapElements[t.element]?.remove(t.loc)
    }
}
