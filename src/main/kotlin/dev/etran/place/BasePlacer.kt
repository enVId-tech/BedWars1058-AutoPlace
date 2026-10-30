package dev.etran.place

import dev.etran.EasySetupPlugin
import dev.etran.bw.ActionQueue
import dev.etran.bw.ElementPlacer
import dev.etran.model.BedInfo
import dev.etran.model.Element
import dev.etran.model.Facing
import dev.etran.model.Frame
import dev.etran.model.Offset
import dev.etran.model.SetupSession
import dev.etran.model.TeamColor
import dev.etran.model.TeamState
import dev.etran.model.Template
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World

/** Queues the placement of one base's elements from the template, anchored on the target bed. */
object BasePlacer {
    private val ORDER = listOf(
        Element.SPAWN, Element.SHOP, Element.UPGRADE, Element.GEN_IRON, Element.GEN_GOLD, Element.KILL_DROPS
    )

    /** Resolve a template element for a target bed without queueing anything (used for previews/tests). */
    @JvmStatic
    fun locate(t: Template, e: Element, bed: BedInfo, mirror: Boolean, snap: Boolean, shopStep: Double, w: World): Location? {
        val o = t.offset(e) ?: return null
        val f = Frame(bed.x + 0.5, bed.y.toDouble(), bed.z + 0.5, bed.facingYaw.toDouble(), mirror)
        return at(w, f, o, snap, e, shopStep, 0.0)
    }

    /** Queue the placement of [team]'s base at [bed]; returns the labels of template elements that were missing. */
    @JvmStatic
    fun queue(plugin: EasySetupPlugin, s: SetupSession, q: ActionQueue, team: TeamColor, bed: BedInfo, mirror: Boolean): List<String> {
        val w = Bukkit.getWorld(bed.world)
        val skipped = ArrayList<String>()
        if (w == null) return skipped
        val t = s.template
        val snap = plugin.settings().snap()
        val shopStep = FacingPolicy.step(plugin.settings(), s)
        val shopPitch = FacingPolicy.pitch(plugin.settings(), s)
        val frame = Frame(bed.x + 0.5, bed.y.toDouble(), bed.z + 0.5, bed.facingYaw.toDouble(), mirror)
        val ts = s.team(team)
        val removeCmd = plugin.bw().build("remove-generator")
        for (e in ORDER) {
            val o = t.offset(e)
            if (o == null) {
                skipped.add(e.label)
                continue
            }
            val target = at(w, frame, o, snap, e, shopStep, shopPitch)
            // A generator of this type already placed at this base: delete it before placing the new one.
            val type = e.genType()
            if (type != null && removeCmd != null) {
                val existing = ElementPlacer.findGen(ts, type, target)
                if (existing != null) {
                    q.teleport(existing.loc)
                    q.command(removeCmd)
                    q.then { ts.gens.remove(existing) }
                }
            }
            q.teleport(target)
            q.command(ElementPlacer.commandFor(plugin.bw(), team, e))
            q.then {
                ts.placed.add(e)
                if (e.isGenerator()) ts.gens.add(TeamState.GenRecord(e.genType()!!, target.clone()))
                else ts.pos[e] = target.clone()
            }
        }
        // Bed last: stand on it so the setup command picks up the right block.
        q.teleport(bed.footCenter(w))
        q.command(ElementPlacer.commandFor(plugin.bw(), team, Element.BED))
        q.then {
            ts.placed.add(Element.BED)
            ts.pos[Element.BED] = bed.footCenter(w)
            ts.bed = bed
            s.addAssigned(bed)
        }
        return skipped
    }

    private fun at(w: World, f: Frame, o: Offset, snap: Boolean, e: Element, shopStep: Double, shopPitch: Double): Location {
        val v = f.toWorld(o)
        var x = v[0]
        var z = v[2]
        if (snap) {
            x = Math.round(x * 2.0) / 2.0
            z = Math.round(z * 2.0) / 2.0
        }
        val shop = Frame.locksFacing(e)
        val yaw = if (shop) Facing.snap(v[3], shopStep) else v[3]
        return Location(w, x, v[1], z, yaw.toFloat(), if (shop) shopPitch.toFloat() else 0f)
    }
}
