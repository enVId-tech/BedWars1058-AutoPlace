package dev.etran.model

import org.bukkit.Location
import java.util.Collections
import java.util.EnumMap

/**
 * The first base the user sets up. Raw world positions are stored as captured; deltas (position and
 * rotation) are always derived on demand relative to the origin bed, so the bed can be resolved after the
 * other elements were captured.
 */
class Template {
    class Raw(val x: Double, val y: Double, val z: Double, val yaw: Double)

    private val raw = EnumMap<Element, Raw>(Element::class.java)
    var team: TeamColor? = null
    var bed: BedInfo? = null

    fun captured(e: Element): Boolean = raw.containsKey(e)

    fun rawView(): Map<Element, Raw> = Collections.unmodifiableMap(raw)

    /** Capture a point: x/y/z snapped to 0.5, shop/upgrade yaw snapped to [shopStep] (0 keeps it), the rest to 90. */
    @JvmOverloads
    fun capture(e: Element, loc: Location, snap: Boolean, shopStep: Double = 45.0) {
        if (!e.capturable()) return
        val x = if (snap) half(loc.x) else loc.x
        val y = if (snap) half(loc.y) else loc.y
        val z = if (snap) half(loc.z) else loc.z
        val yaw = if (Frame.locksFacing(e)) Facing.snap(loc.yaw.toDouble(), shopStep) else Facing.snap(loc.yaw.toDouble(), 90.0)
        raw[e] = Raw(x, y, z, yaw)
    }

    fun frame(): Frame? {
        val b = bed ?: return null
        return Frame(b.x + 0.5, b.y.toDouble(), b.z + 0.5, b.facingYaw.toDouble(), false)
    }

    /** Offset of the element from the origin bed, or null if the element or the bed is not known yet. */
    fun offset(e: Element): Offset? {
        val r = raw[e]
        val f = frame()
        if (r == null || f == null) return null
        return f.toLocal(r.x, r.y, r.z, r.yaw)
    }

    /** Forget a captured element (it was deleted). */
    fun remove(e: Element) {
        raw.remove(e)
    }

    fun clear() {
        raw.clear()
        bed = null
        team = null
    }

    private fun half(v: Double): Double = Math.round(v * 2.0) / 2.0
}
