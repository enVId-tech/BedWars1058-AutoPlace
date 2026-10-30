package dev.etran.scan

import dev.etran.Settings
import dev.etran.compat.Compat
import dev.etran.compat.Dyes
import dev.etran.model.BedInfo
import dev.etran.model.TeamColor
import org.bukkit.World

/**
 * Works out which team colour each base is. Every block near a bed is credited to its nearest bed; a base's
 * score for a colour is its own count minus the mean count at the other bases, so colour shared by all bases
 * (decoration) cancels out. Colours are then assigned uniquely (best total score for up to 6 bases).
 */
object WoolDetector {
    class Result(val byBed: Map<String, TeamColor>, val counts: Map<String, IntArray>)

    @JvmStatic
    fun detect(w: World, beds: List<BedInfo>, st: Settings, allowed: Collection<TeamColor>): Result {
        val n = beds.size
        val cols = TeamColor.entries.toTypedArray()
        val counts = Array(n) { IntArray(cols.size) }
        val out = HashMap<String, TeamColor>()
        val countsByBed = HashMap<String, IntArray>()
        if (n == 0) return Result(out, countsByBed)
        val radius = st.woolRadius()
        val minX = beds.minOf { it.x }
        val maxX = beds.maxOf { it.x }
        val minZ = beds.minOf { it.z }
        val maxZ = beds.maxOf { it.z }
        val snap = WorldSnap(w, minX - radius, minZ - radius, maxX + radius, maxZ + radius)
        val families = st.woolFamilies()
        for (i in 0 until n) {
            val b = beds[i]
            for (x in (b.x - radius)..(b.x + radius)) {
                for (z in (b.z - radius)..(b.z + radius)) {
                    if (nearestBed(beds, x, z) != i) continue
                    for (y in (b.y - 12)..(b.y + 16)) {
                        val m = snap.type(x, y, z)
                        var data = 0
                        if (!Compat.FLATTENED && Dyes.isLegacyFamily(m, families)) data = snap.legacyData(x, y, z)
                        val dye = Dyes.dyeOf(m, data, families) ?: continue
                        val c = TeamColor.fromDye(dye)
                        if (c != null) counts[i][c.ordinal]++
                    }
                }
            }
        }
        val score = Array(n) { DoubleArray(cols.size) }
        for (i in 0 until n) {
            for (c in cols.indices) {
                var others = 0.0
                for (j in 0 until n) if (j != i) others += counts[j][c]
                val mean = if (n > 1) others / (n - 1) else 0.0
                score[i][c] = counts[i][c] - mean
            }
        }
        val ok = BooleanArray(cols.size) { allowed.isEmpty() || allowed.contains(cols[it]) }
        var assign = IntArray(n) { -1 }
        if (n <= 6) {
            val best = IntArray(n) { -1 }
            val bestVal = doubleArrayOf(Double.NEGATIVE_INFINITY)
            search(0, 0, 0.0, IntArray(n), score, ok, st.woolMinScore().toDouble(), best, bestVal)
            assign = best
        } else {
            val used = BooleanArray(cols.size)
            for (i in 0 until n) {
                var pick = -1
                var pv = st.woolMinScore().toDouble()
                for (c in cols.indices) {
                    if (ok[c] && !used[c] && score[i][c] >= pv) {
                        pv = score[i][c]
                        pick = c
                    }
                }
                if (pick >= 0) {
                    used[pick] = true
                    assign[i] = pick
                }
            }
        }
        for (i in 0 until n) {
            countsByBed[beds[i].key()] = counts[i]
            if (assign[i] >= 0) out[beds[i].key()] = cols[assign[i]]
        }
        return Result(out, countsByBed)
    }

    private fun search(
        i: Int, usedMask: Int, cur: Double, path: IntArray, score: Array<DoubleArray>, ok: BooleanArray,
        min: Double, best: IntArray, bestVal: DoubleArray
    ) {
        val n = path.size
        if (i == n) {
            if (cur > bestVal[0]) {
                bestVal[0] = cur
                System.arraycopy(path, 0, best, 0, n)
            }
            return
        }
        path[i] = -1 // leave this base unassigned
        search(i + 1, usedMask, cur, path, score, ok, min, best, bestVal)
        for (c in score[i].indices) {
            if (!ok[c] || (usedMask and (1 shl c)) != 0 || score[i][c] < min) continue
            path[i] = c
            search(i + 1, usedMask or (1 shl c), cur + score[i][c], path, score, ok, min, best, bestVal)
        }
        path[i] = -1
    }

    private fun nearestBed(beds: List<BedInfo>, x: Int, z: Int): Int {
        var best = 0
        var bestD = Long.MAX_VALUE
        for (i in beds.indices) {
            val dx = (x - beds[i].x).toLong()
            val dz = (z - beds[i].z).toLong()
            val d = dx * dx + dz * dz
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        return best
    }
}
