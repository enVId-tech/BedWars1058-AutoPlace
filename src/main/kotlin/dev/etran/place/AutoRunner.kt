package dev.etran.place

import dev.etran.EasySetupPlugin
import dev.etran.Msg
import dev.etran.Settings
import dev.etran.bw.ActionQueue
import dev.etran.bw.ElementPlacer
import dev.etran.model.BedInfo
import dev.etran.model.Element
import dev.etran.model.SetupMode
import dev.etran.model.SetupSession
import dev.etran.model.TeamColor
import dev.etran.scan.BedScanner
import dev.etran.scan.GeneratorScanner
import dev.etran.scan.LobbyDetector
import dev.etran.scan.MirrorDetector
import dev.etran.scan.WoolDetector
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import kotlin.math.atan2

/**
 * Copies the hand-made template base onto the other bases (`/bwauto`), one at a time (semi-automatic) or all
 * at once with lobby and generator detection (fully automatic).
 */
object AutoRunner {
    @JvmStatic
    fun request(plugin: EasySetupPlugin, p: Player, singleBase: Boolean) {
        val s = plugin.sessions().get(p)
        if (s.mode == SetupMode.MANUAL) {
            Msg.err(p, "Automatic placement is disabled in Manual mode. Switch mode in /bwsetup.")
            return
        }
        if (s.busy) {
            Msg.err(p, "Another placement is still running - wait a moment.")
            return
        }
        if (s.gamemode == null) {
            Msg.err(p, "Pick a gamemode first (the nether star in /bwsetup).")
            return
        }
        val missing = Validator.missingTemplate(s)
        if (missing.isNotEmpty() && !s.confirmedIncomplete) {
            s.pendingAuto = true
            s.pendingSingle = singleBase
            p.sendMessage(ChatColor.RED.toString() + plugin.settings().incompleteAutoMessage(Element.join(missing)))
            return
        }
        s.confirmedIncomplete = false
        s.pendingAuto = false
        if (singleBase) nextBase(plugin, p, s) else runAll(plugin, p, s)
    }

    @JvmStatic
    fun confirm(plugin: EasySetupPlugin, p: Player) {
        val s = plugin.sessions().get(p)
        if (!s.pendingAuto) {
            Msg.info(p, "Nothing is waiting for confirmation.")
            return
        }
        s.confirmedIncomplete = true
        request(plugin, p, s.pendingSingle)
    }

    @JvmStatic
    fun deny(plugin: EasySetupPlugin, p: Player) {
        val s = plugin.sessions().get(p)
        if (!s.pendingAuto) {
            Msg.info(p, "Nothing is waiting for confirmation.")
            return
        }
        s.pendingAuto = false
        s.confirmedIncomplete = false
        Msg.info(p, "Automatic placement cancelled. Place the missing elements, then run /bwauto again.")
    }

    // ------------------------------------------------------------------------------------------
    // shared preparation
    // ------------------------------------------------------------------------------------------

    private fun prepare(plugin: EasySetupPlugin, p: Player, s: SetupSession): Boolean {
        for (c in s.requiredTeams()) {
            if (!TeamManager.requireTeam(p, s, c)) return false
        }
        val t = s.template
        if (t.team == null) t.team = s.selected ?: s.requiredTeams()[0]
        if (t.bed == null) {
            val b = BedScanner.nearest(p.world, p.location, plugin.settings().templateBedRadius())
            if (b != null) {
                t.bed = b
                s.addAssigned(b)
            }
        }
        if (t.bed == null) {
            Msg.err(p, "No template bed found. Stand near the base you set up, or place its bed element first.")
            return false
        }
        if (t.offset(Element.SPAWN) == null) {
            Msg.err(p, "The template base has no spawn captured, so there is nothing to copy. Place the spawn first.")
            return false
        }
        return true
    }

    private fun centre(st: Settings, w: World, ref: Location): Location = Location(w, st.centerX(), ref.y, st.centerZ())

    // ------------------------------------------------------------------------------------------
    // fully automatic
    // ------------------------------------------------------------------------------------------

    private fun runAll(plugin: EasySetupPlugin, p: Player, s: SetupSession) {
        if (!prepare(plugin, p, s)) return
        val st = plugin.settings()
        val w = p.world
        val t = s.template
        val tbed = t.bed!!
        val start = p.location.clone()
        val unassigned = BedScanner.unassigned(s, st, w, centre(st, w, start))
        val all = ArrayList<BedInfo>()
        all.add(tbed)
        all.addAll(unassigned)
        val wool = WoolDetector.detect(w, all, st, s.requiredTeams())
        val seen = wool.byBed[tbed.key()]
        if (seen != null && seen != t.team) {
            Msg.info(p, "The wool at the template base looks ${seen.display} but you set it up as ${t.team!!.display}.")
        }
        val remaining = remainingTeams(s)
        val plan = assign(unassigned, remaining, wool)
        if (unassigned.size > remaining.size) {
            Msg.info(p, "Found ${unassigned.size} unassigned beds for ${remaining.size} remaining team(s); extra beds are ignored.")
        } else if (unassigned.size < remaining.size) {
            Msg.info(p, "Only ${unassigned.size} unassigned bed(s) found for ${remaining.size} remaining team(s).")
        }
        p.closeInventory()
        val q = ActionQueue(plugin, p)
        val summary = StringBuilder()
        for ((bed, team) in plan) {
            val mirror = MirrorDetector.isMirrored(w, tbed, bed, st, s)
            val skipped = BasePlacer.queue(plugin, s, q, team, bed, mirror)
            summary.append(team.display).append(if (mirror) " (mirrored) " else " ")
            if (skipped.isNotEmpty()) {
                Msg.info(p, team.display + ": template has no " + skipped.joinToString(", ") + ".")
            }
        }
        queueLobby(plugin, p, s, q, w)
        val gens = queueGenerators(plugin, p, s, q, w, start)
        s.busy = true
        q.teleport(start)
        q.done {
            s.busy = false
            Msg.ok(
                p,
                "Automatic placement finished. Bases: " +
                    (if (summary.isEmpty()) "none" else summary.toString().trim()) +
                    ". Generators queued: ${gens[0]} diamond, ${gens[1]} emerald."
            )
            Msg.info(p, "Open /bwsetup and use the green concrete to validate and save.")
        }
        q.start()
    }

    // ------------------------------------------------------------------------------------------
    // semi-automatic: one base, then walk to the next bed
    // ------------------------------------------------------------------------------------------

    private fun nextBase(plugin: EasySetupPlugin, p: Player, s: SetupSession) {
        if (!prepare(plugin, p, s)) return
        val st = plugin.settings()
        val w = p.world
        val t = s.template
        val tbed = t.bed!!
        val start = p.location.clone()
        val unassigned = BedScanner.unassigned(s, st, w, centre(st, w, start))
        if (unassigned.isEmpty()) {
            Msg.info(p, "No further unassigned base islands detected on the map.")
            return
        }
        val target = unassigned.minByOrNull { it.footCenter(w).distanceSquared(start) } ?: unassigned[0]
        val all = ArrayList<BedInfo>()
        all.add(tbed)
        all.addAll(unassigned)
        val wool = WoolDetector.detect(w, all, st, s.requiredTeams())
        val remaining = remainingTeams(s)
        if (remaining.isEmpty()) {
            Msg.info(p, "Every required team already has a bed.")
            return
        }
        var color = wool.byBed[target.key()]
        if (color == null || !remaining.contains(color)) color = remaining[0]
        val mirror = MirrorDetector.isMirrored(w, tbed, target, st, s)
        p.closeInventory()
        val q = ActionQueue(plugin, p)
        val skipped = BasePlacer.queue(plugin, s, q, color, target, mirror)
        if (skipped.isNotEmpty()) Msg.info(p, "Template has no " + skipped.joinToString(", ") + ".")
        val placed = color
        s.busy = true
        q.teleport(start)
        q.done {
            s.busy = false
            Msg.ok(p, "Placed team ${placed.display}" + (if (mirror) " (mirrored)" else "") + ".")
            val left = BedScanner.unassigned(s, st, w, centre(st, w, start)).filter { it.key() != target.key() }
            if (left.isEmpty()) {
                Msg.info(p, "No further unassigned base islands detected on the map.")
            } else {
                val next = left.minByOrNull { it.footCenter(w).distanceSquared(p.location) } ?: left[0]
                p.teleport(standOn(next, st, w))
                Msg.ok(p, "Teleported onto the bed of the next unassigned base island.")
            }
        }
        q.start()
    }

    /** Standing on the bed, facing the map centre, snapped to 90 degrees. */
    private fun standOn(bed: BedInfo, st: Settings, w: World): Location {
        val l = bed.footCenter(w).add(0.0, 1.0, 0.0)
        val dx = st.centerX() - l.x
        val dz = st.centerZ() - l.z
        var yaw = Math.toDegrees(atan2(-dx, dz))
        yaw = Math.round(yaw / 90.0) * 90.0
        l.yaw = yaw.toFloat()
        l.pitch = 0f
        return l
    }

    // ------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------

    /** Required teams that have no bed yet (the template team is excluded - it is already set up). */
    internal fun remainingTeams(s: SetupSession): List<TeamColor> =
        s.requiredTeams().filter { it != s.template.team && !s.teamPlaced(it, Element.BED) }

    /** Prefer the colour detected from each base's wool; hand out leftover teams in order. */
    internal fun assign(beds: List<BedInfo>, teams: List<TeamColor>, wool: WoolDetector.Result): Map<BedInfo, TeamColor> {
        val plan = LinkedHashMap<BedInfo, TeamColor>()
        val pool = ArrayList(teams)
        for (b in beds) {
            val c = wool.byBed[b.key()]
            if (c != null && pool.remove(c)) plan[b] = c
        }
        for (b in beds) {
            if (plan.containsKey(b) || pool.isEmpty()) continue
            plan[b] = pool.removeAt(0)
        }
        return plan
    }

    internal fun queueLobby(plugin: EasySetupPlugin, p: Player, s: SetupSession, q: ActionQueue, w: World) {
        val wanted = s.mode == SetupMode.AUTO || s.autoLobby
        val haveAll = s.mapPlaced(Element.WAITING_SPAWN) && s.mapPlaced(Element.SPECTATOR_SPAWN) &&
            s.mapPlaced(Element.LOBBY_POS1) && s.mapPlaced(Element.LOBBY_POS2)
        if (!wanted || haveAll) return
        queueLobbyDetection(plugin, p, s, q, w)
    }

    /** Detect the lobby structure and queue its markers. Also used directly by the lobby menu. */
    @JvmStatic
    fun queueLobbyDetection(plugin: EasySetupPlugin, p: Player, s: SetupSession, q: ActionQueue, w: World): Boolean {
        val r = LobbyDetector.detect(w, plugin.settings())
        if (r == null) {
            Msg.err(p, "Could not detect a lobby structure near the map centre. Place the lobby markers by hand (Lobby & Boundary menu).")
            return false
        }
        val centre = r.center()
        val c1 = r.corner1()
        val c2 = r.corner2()
        val bw = plugin.bw()
        q.teleport(centre)
        q.command(ElementPlacer.commandFor(bw, null, Element.WAITING_SPAWN))
        q.then { s.setMap(Element.WAITING_SPAWN, centre) }
        q.command(ElementPlacer.commandFor(bw, null, Element.SPECTATOR_SPAWN))
        q.then { s.setMap(Element.SPECTATOR_SPAWN, centre) }
        q.teleport(c1)
        q.command(ElementPlacer.commandFor(bw, null, Element.LOBBY_POS1))
        q.then { s.setMap(Element.LOBBY_POS1, c1) }
        q.teleport(c2)
        q.command(ElementPlacer.commandFor(bw, null, Element.LOBBY_POS2))
        q.then { s.setMap(Element.LOBBY_POS2, c2) }
        Msg.info(
            p,
            "Lobby structure detected (${r.blocks} blocks): box (${r.minX}, ${r.minY}, ${r.minZ}) to " +
                "(${r.maxX}, ${r.maxY}, ${r.maxZ}), spawns at its centre."
        )
        return true
    }

    /** Queue placement of every detectable, still-unplaced diamond and emerald generator. */
    @JvmStatic
    fun queueGenerators(plugin: EasySetupPlugin, p: Player, s: SetupSession, q: ActionQueue, w: World, around: Location): IntArray {
        val counts = IntArray(2)
        val types = arrayOf("diamond", "emerald")
        val els = arrayOf(Element.GEN_DIAMOND, Element.GEN_EMERALD)
        for (i in 0 until 2) {
            val type = types[i]
            val el = els[i]
            val found = GeneratorScanner.findUnplaced(w, around, type, plugin.settings(), s)
            for (spot in found) {
                q.teleport(spot)
                q.command(ElementPlacer.commandFor(plugin.bw(), null, el))
                q.then { s.addMapGen(el, spot) }
                counts[i]++
            }
            if (found.isEmpty()) Msg.info(p, "No unplaced $type generators detected.")
        }
        return counts
    }

    /** Used by menus to know how many team slots are left. */
    @JvmStatic
    fun remainingCount(s: SetupSession): Int = remainingTeams(s).size
}
