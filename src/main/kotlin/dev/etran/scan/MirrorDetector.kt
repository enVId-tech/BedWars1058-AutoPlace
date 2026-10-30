package dev.etran.scan

import dev.etran.Settings
import dev.etran.model.BedInfo
import dev.etran.model.Frame
import dev.etran.model.Offset
import dev.etran.model.SetupSession
import org.bukkit.Material
import org.bukkit.World
import kotlin.math.floor

/**
 * Decides whether a target base is a mirror image of the template base (rather than a rotated copy) by
 * comparing the surrounding blocks, colour-blind, under both hypotheses.
 */
object MirrorDetector {
    @JvmStatic
    fun isMirrored(w: World, template: BedInfo, target: BedInfo, st: Settings, s: SetupSession): Boolean {
        val mode = st.mirrorMode()
        if (mode == "off") return false
        if (mode == "force") return true
        if (template.key() == target.key()) return false
        val cacheKey = template.key() + ">" + target.key()
        s.mirrorCache[cacheKey]?.let { return it }

        val ft = Frame(template.x + 0.5, template.y.toDouble(), template.z + 0.5, template.facingYaw.toDouble(), false)
        val fr = Frame(target.x + 0.5, target.y.toDouble(), target.z + 0.5, target.facingYaw.toDouble(), false)
        val fm = Frame(target.x + 0.5, target.y.toDouble(), target.z + 0.5, target.facingYaw.toDouble(), true)
        val r = st.mirrorRadius()
        var total = 0
        var rotHits = 0
        var mirHits = 0
        for (right in -r..r) {
            for (fwd in -r..r) {
                for (up in -4..8) {
                    val o = Offset(right.toDouble(), up.toDouble(), fwd.toDouble(), 0.0)
                    val tm = blockAt(w, ft, o)
                    if (Mats.isAir(tm)) continue
                    total++
                    val fam = Mats.family(tm)
                    if (Mats.family(blockAt(w, fr, o)) == fam) rotHits++
                    if (Mats.family(blockAt(w, fm, o)) == fam) mirHits++
                }
            }
        }
        var mirrored = false
        if (total > 0) {
            val rot = rotHits / total.toDouble()
            val mir = mirHits / total.toDouble()
            mirrored = mir > rot + st.mirrorMargin()
        }
        s.mirrorCache[cacheKey] = mirrored
        return mirrored
    }

    private fun blockAt(w: World, f: Frame, o: Offset): Material {
        val p = f.toWorld(o)
        return w.getBlockAt(floor(p[0]).toInt(), floor(p[1] + 1e-6).toInt(), floor(p[2]).toInt()).type
    }
}
