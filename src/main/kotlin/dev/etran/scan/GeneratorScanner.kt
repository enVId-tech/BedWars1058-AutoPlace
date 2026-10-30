package dev.etran.scan

import dev.etran.Settings
import dev.etran.infer.GeneratorDetector
import dev.etran.infer.Kind
import dev.etran.model.Element
import dev.etran.model.SetupSession
import org.bukkit.Location
import org.bukkit.World
import kotlin.math.floor

/** Finds diamond / emerald generator spots from the blocks of the map (see [GeneratorDetector]). */
object GeneratorScanner {
    /** Scores every candidate around [around]; nothing is filtered. */
    @JvmStatic
    fun scan(w: World, around: Location, st: Settings): GeneratorDetector.Result {
        val r = st.generatorScanRadius()
        val cx = floor(st.centerX()).toInt()
        val cz = floor(st.centerZ()).toInt()
        val snap = WorldSnap(w, cx - r, cz - r, cx + r, cz + r)
        val yr = st.generatorYRange()
        val y0 = maxOf(snap.minY + 1, around.blockY - yr)
        val y1 = minOf(snap.maxY - 4, around.blockY + yr)
        return GeneratorDetector.detect(
            WorldGrid(snap), cx - r, cx + r, y0, y1, cz - r, cz + r, st.centerX(), st.centerZ()
        )
    }

    /** Accepted, not yet placed generators of one type ("diamond" or "emerald"), nearest to [around] first. */
    @JvmStatic
    fun findUnplaced(w: World, around: Location, type: String, st: Settings, s: SetupSession): List<Location> {
        val target = if (type == "diamond") Kind.DIAMOND_BLOCK else Kind.EMERALD_BLOCK
        val el = if (type == "diamond") Element.GEN_DIAMOND else Element.GEN_EMERALD
        val res = scan(w, around, st)
        val out = ArrayList<Location>()
        for (c in res.candidates) {
            if (c.type != target || c.p < st.generatorMinProbability()) continue
            val spot = Location(w, c.x, c.y, c.z)
            if (BedScanner.armorStandNear(w, spot, 1.5) || alreadyRecorded(s, el, spot)) continue
            out.add(spot)
        }
        out.sortBy { it.distanceSquared(around) }
        return out
    }

    /** One chat line per candidate worth showing, for /bwauto gens. */
    @JvmStatic
    fun describe(res: GeneratorDetector.Result, st: Settings, w: World, s: SetupSession): List<String> {
        val lines = ArrayList<String>()
        var shown = 0
        for (c in res.candidates) {
            if (c.p < 0.15 || shown >= 14) continue
            val spot = Location(w, c.x, c.y, c.z)
            val el = if (c.isDiamond()) Element.GEN_DIAMOND else Element.GEN_EMERALD
            val state = if (alreadyRecorded(s, el, spot) || BedScanner.armorStandNear(w, spot, 1.5)) "placed"
            else if (c.p >= st.generatorMinProbability()) "ACCEPT" else "skip"
            lines.add(
                String.format(
                    "[%s] %.2f %s at %.1f %.0f %.1f  (%s)", state, c.p,
                    if (c.isDiamond()) "diamond" else "emerald", c.x, c.y, c.z, c.why
                )
            )
            shown++
        }
        return lines
    }

    private fun alreadyRecorded(s: SetupSession, el: Element, spot: Location): Boolean {
        val l = s.mapElements[el] ?: return false
        return l.any { it.world != null && it.world == spot.world && it.distance(spot) < 1.5 }
    }
}
