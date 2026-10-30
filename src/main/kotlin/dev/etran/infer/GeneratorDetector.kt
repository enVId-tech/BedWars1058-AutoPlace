package dev.etran.infer

import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Finds diamond / emerald generator spots in a block grid and gives each a probability.
 *
 * Every diamond or emerald block (grouped with touching blocks of the same kind) is a candidate. Independent
 * clues are combined in log-odds: free headroom above, a pedestal (stairs/slabs around it or lower ground at its
 * sides), support below, small size, map symmetry (partners under 90/180/270 degree rotation or an axis mirror
 * about the map centre) and distance from beds.
 */
object GeneratorDetector {
    class Candidate internal constructor(
        @JvmField val type: Kind,
        /** Where the player should stand: on top of the block (centre of the cluster, snapped to 0.5). */
        @JvmField val x: Double,
        @JvmField val y: Double,
        @JvmField val z: Double,
        @JvmField val size: Int
    ) {
        @JvmField
        var p: Double = 0.0

        @JvmField
        var why: String = ""

        /** Logit before the symmetry clue. */
        internal var base: Double = 0.0

        /** Probability before the symmetry clue. */
        internal var p0: Double = 0.0

        fun isDiamond(): Boolean = type == Kind.DIAMOND_BLOCK
    }

    class Result internal constructor(
        @JvmField val candidates: List<Candidate>,
        /** {x, z} of every bed block seen. */
        @JvmField val beds: List<DoubleArray>,
        @JvmField val symmetric: Boolean
    )

    private const val MAX_CLUSTER = 256
    private const val SYM_TOL = 2.0

    private val N6 = arrayOf(
        intArrayOf(1, 0, 0), intArrayOf(-1, 0, 0), intArrayOf(0, 1, 0),
        intArrayOf(0, -1, 0), intArrayOf(0, 0, 1), intArrayOf(0, 0, -1)
    )
    private val N4 = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))

    @JvmStatic
    fun detect(
        g: BlockGrid, minX: Int, maxX: Int, minY: Int, maxY: Int, minZ: Int, maxZ: Int,
        cx: Double, cz: Double
    ): Result {
        val blocks = ArrayList<IntArray>()
        val beds = ArrayList<DoubleArray>()
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (y in minY..maxY) {
                    val k = g.kind(x, y, z)
                    if (k == Kind.DIAMOND_BLOCK || k == Kind.EMERALD_BLOCK) {
                        blocks.add(intArrayOf(x, y, z))
                    } else if (k == Kind.BED) {
                        beds.add(doubleArrayOf(x + 0.5, z + 0.5))
                    }
                }
            }
        }

        // 6-connected clusters of the same type.
        val seen = HashSet<Long>()
        val out = ArrayList<Candidate>()
        for (b in blocks) {
            val key = key(b[0], b[1], b[2])
            if (seen.contains(key)) continue
            val type = g.kind(b[0], b[1], b[2])
            val cluster = ArrayList<IntArray>()
            val dq = ArrayDeque<IntArray>()
            dq.add(b)
            seen.add(key)
            while (!dq.isEmpty()) {
                val c = dq.poll()
                if (cluster.size < MAX_CLUSTER) cluster.add(c)
                for (d in N6) {
                    val nx = c[0] + d[0]
                    val ny = c[1] + d[1]
                    val nz = c[2] + d[2]
                    if (g.kind(nx, ny, nz) != type) continue
                    if (seen.add(key(nx, ny, nz))) dq.add(intArrayOf(nx, ny, nz))
                }
            }
            out.add(score(g, type, cluster, beds))
        }

        // Pass 2: symmetry.
        val strong = out.filter { it.p0 >= 0.5 }
        val matched = IntArray(out.size) { partner(out, out[it], cx, cz) } // 1 partner, 0 none, -1 on the centre
        var matchedStrong = 0
        for (i in out.indices) {
            if (out[i].p0 >= 0.5 && matched[i] == 1) matchedStrong++
        }
        val symmetric = strong.size >= 3 && matchedStrong >= 0.5 * strong.size
        for (i in out.indices) {
            val c = out[i]
            var z = c.base
            var sym = "n/a"
            if (symmetric && matched[i] == -1) {
                sym = "on centre"
            } else if (symmetric) {
                z += if (matched[i] == 1) 1.3 else -1.2
                sym = if (matched[i] == 1) "partner" else "none"
            }
            c.p = logistic(z)
            c.why = c.why + String.format(" sym=%s -> p=%.2f", sym, c.p)
        }
        out.sortWith { a, b -> java.lang.Double.compare(b.p, a.p) }
        return Result(out, beds, symmetric)
    }

    private fun score(g: BlockGrid, type: Kind, cluster: List<IntArray>, beds: List<DoubleArray>): Candidate {
        val n = cluster.size
        var top = Int.MIN_VALUE
        var bottom = Int.MAX_VALUE
        for (c in cluster) {
            top = max(top, c[1])
            bottom = min(bottom, c[1])
        }
        val topCells = ArrayList<IntArray>()
        val bottomCells = ArrayList<IntArray>()
        var sx = 0.0
        var sz = 0.0
        for (c in cluster) {
            if (c[1] == top) {
                topCells.add(c)
                sx += c[0] + 0.5
                sz += c[2] + 0.5
            }
            if (c[1] == bottom) bottomCells.add(c)
        }
        val px = Math.round(sx / topCells.size * 2.0) / 2.0
        val pz = Math.round(sz / topCells.size * 2.0) / 2.0

        // headroom: consecutive passable blocks above (max 3), averaged over the top cells
        var air = 0.0
        for (c in topCells) {
            var k = 0
            while (k < 3 && g.kind(c[0], top + 1 + k, c[2]).passable()) k++
            air += k / 3.0
        }
        air /= topCells.size

        // support: solid below the lowest layer
        var ground = 0.0
        for (c in bottomCells) {
            if (g.kind(c[0], bottom - 1, c[2]).ground()) ground += 1.0
        }
        ground /= bottomCells.size

        // pedestal: ring of orthogonal neighbours around the top layer
        val inCluster = HashSet<Long>()
        for (c in cluster) inCluster.add(key(c[0], c[1], c[2]))
        var ringN = 0
        var ringPartialTop = 0
        var ringRaised = 0
        var ringPartialAbove = 0
        val ringSeen = HashSet<Long>()
        for (c in topCells) {
            for (d in N4) {
                val nx = c[0] + d[0]
                val nz = c[2] + d[1]
                if (inCluster.contains(key(nx, top, nz))) continue
                if (!ringSeen.add(key(nx, 0, nz))) continue
                ringN++
                val atTop = g.kind(nx, top, nz)
                if (atTop.partial()) ringPartialTop++
                else if (atTop.passable()) ringRaised++
                if (g.kind(nx, top + 1, nz).partial()) ringPartialAbove++
            }
        }
        val ped = if (ringN > 0) max(ringPartialAbove, max(ringPartialTop, ringRaised)) / ringN.toDouble() else 0.0

        val sizeTerm = if (n == 1) 0.0 else if (n <= 4) -0.8 else if (n <= 9) -2.0 else -4.0
        var bedD = Double.MAX_VALUE
        for (b in beds) bedD = min(bedD, hypot(b[0] - px, b[1] - pz))
        val bedTerm = if (bedD < 5) -1.5 else if (bedD < 9) -0.6 else 0.0

        val z = -3.0 + 2.0 * air + 1.6 * ped + 1.0 * ground + sizeTerm + bedTerm
        val cand = Candidate(type, px, (top + 1).toDouble(), pz, n)
        cand.base = z
        cand.p0 = logistic(z)
        cand.why = String.format(
            "%s x%d air=%.2f pedestal=%.2f support=%.2f bed=%s", type.name, n, air, ped, ground,
            if (bedD == Double.MAX_VALUE) "-" else String.format("%.0f", bedD)
        )
        return cand
    }

    /** 1 = a partner exists, 0 = none, -1 = every image maps onto the candidate itself (it sits on the centre). */
    private fun partner(all: List<Candidate>, c: Candidate, cx: Double, cz: Double): Int {
        val dx = c.x - cx
        val dz = c.z - cz
        val images = arrayOf(
            doubleArrayOf(-dz, dx), doubleArrayOf(-dx, -dz), doubleArrayOf(dz, -dx), // 90, 180, 270
            doubleArrayOf(-dx, dz), doubleArrayOf(dx, -dz) // mirror across the z axis / x axis
        )
        var nontrivial = 0
        for (im in images) {
            val tx = cx + im[0]
            val tz = cz + im[1]
            if (hypot(tx - c.x, tz - c.z) <= SYM_TOL) continue // maps onto itself: no evidence
            nontrivial++
            for (o in all) {
                if (o === c || o.type != c.type || o.p0 < 0.35) continue
                if (hypot(o.x - tx, o.z - tz) <= SYM_TOL && abs(o.y - c.y) <= 3) return 1
            }
        }
        return if (nontrivial == 0) -1 else 0
    }

    private fun logistic(z: Double): Double = 1.0 / (1.0 + exp(-z))

    private fun key(x: Int, y: Int, z: Int): Long =
        ((x and 0x3FFFFFF).toLong() shl 38) or ((z and 0x3FFFFFF).toLong() shl 12) or ((y + 2048) and 0xFFF).toLong()
}
