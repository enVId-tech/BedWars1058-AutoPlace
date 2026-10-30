package dev.etran.scan

import dev.etran.Settings
import dev.etran.compat.BedCompat
import dev.etran.compat.Compat
import dev.etran.model.BedInfo
import dev.etran.model.SetupSession
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import kotlin.math.atan2
import kotlin.math.sqrt

/** Finds beds in the world and decides which of them are still unassigned. */
object BedScanner {
    private const val Y_RANGE_FALLBACK = 80

    /** All beds within [radius] blocks (3D) of the centre. */
    @JvmStatic
    @JvmOverloads
    fun scan(w: World, center: Location, radius: Int, yRange: Int = Y_RANGE_FALLBACK): List<BedInfo> {
        val out = LinkedHashMap<String, BedInfo>()
        val minY = Compat.minY(w)
        val maxY = Compat.maxY(w)
        // Small searches (nearest bed) use the radius itself as the vertical window; big map scans use yRange.
        val half = if (radius <= 64) radius else yRange
        val y0 = maxOf(minY, center.blockY - half)
        val y1 = minOf(maxY - 1, center.blockY + half)
        val ccx = center.blockX shr 4
        val ccz = center.blockZ shr 4
        val cr = (radius shr 4) + 1
        for (cx in (ccx - cr)..(ccx + cr)) {
            for (cz in (ccz - cr)..(ccz + cr)) {
                val dx = (cx * 16 + 8) - center.x
                val dz = (cz * 16 + 8) - center.z
                if (sqrt(dx * dx + dz * dz) > radius + 12) continue
                val snap = WorldSnap.snapshot(w, cx, cz) ?: continue
                for (lx in 0 until 16) {
                    for (lz in 0 until 16) {
                        for (y in y0..y1) {
                            val m = WorldSnap.type(snap, lx, y, lz)
                            if (!Mats.isBed(m)) continue
                            val bi = BedCompat.resolve(w.getBlockAt(cx * 16 + lx, y, cz * 16 + lz)) ?: continue
                            if (bi.footCenter(w).distance(center) > radius) continue
                            if (!out.containsKey(bi.key())) out[bi.key()] = bi
                        }
                    }
                }
            }
        }
        return ArrayList(out.values)
    }

    /** The bed nearest to [loc] within [radius], or null. */
    @JvmStatic
    fun nearest(w: World, loc: Location, radius: Int): BedInfo? =
        scan(w, loc, radius).minByOrNull { it.footCenter(w).distance(loc) }

    @JvmStatic
    fun armorStandNear(w: World, at: Location, radius: Double): Boolean {
        for (e in w.getNearbyEntities(at, radius, radius, radius)) {
            if (e is ArmorStand && e.location.distance(at) <= radius) return true
        }
        return false
    }

    @JvmStatic
    fun unassigned(s: SetupSession, st: Settings, w: World, center: Location): List<BedInfo> {
        val out = ArrayList<BedInfo>()
        for (b in scan(w, center, st.bedScanRadius(), st.bedScanYRange())) {
            val skip = s.assignedBeds.any { it.key() == b.key() || it.distanceTo(b) < st.assignedBedRadius() }
            if (skip) continue
            if (armorStandNear(w, b.footCenter(w), st.bedArmorStandRadius())) continue
            out.add(b)
        }
        val cx = st.centerX()
        val cz = st.centerZ()
        out.sortBy { atan2(it.z + 0.5 - cz, it.x + 0.5 - cx) }
        return out
    }
}
