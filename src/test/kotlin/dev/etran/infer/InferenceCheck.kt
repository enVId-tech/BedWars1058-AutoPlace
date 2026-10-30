package dev.etran.infer

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.system.exitProcess

/**
 * Standalone check of the generator detector on a synthetic map (no server needed):
 * `./gradlew inferenceCheck`. Exits non-zero on any failure.
 *
 * Map: four beds around (0,0); 4 diamond pedestals, 1 emerald pedestal at the centre, 4 flush emeralds,
 * and decoys (a solid diamond cube, a diamond set into a wall beside a bed, a lone flush diamond).
 */
object InferenceCheck {
    private var failures = 0
    private const val BY = 64

    private fun check(ok: Boolean, what: String) {
        println((if (ok) "PASS " else "FAIL ") + what)
        if (!ok) failures++
    }

    private class Grid : BlockGrid {
        val m = HashMap<String, Kind>()
        fun set(x: Int, y: Int, z: Int, k: Kind) {
            m["$x,$y,$z"] = k
        }

        override fun kind(x: Int, y: Int, z: Int): Kind = m["$x,$y,$z"] ?: Kind.AIR
    }

    private fun pedestal(g: Grid, x: Int, z: Int, k: Kind) {
        g.set(x, BY - 1, z, Kind.SOLID)
        g.set(x, BY, z, k)
        g.set(x + 1, BY + 1, z, Kind.STAIRS)
        g.set(x - 1, BY + 1, z, Kind.STAIRS)
        g.set(x, BY + 1, z + 1, Kind.STAIRS)
        g.set(x, BY + 1, z - 1, Kind.STAIRS)
    }

    private fun flush(g: Grid, x: Int, z: Int, k: Kind) {
        for (dx in -2..2) for (dz in -2..2) {
            g.set(x + dx, BY - 1, z + dz, Kind.SOLID)
            g.set(x + dx, BY - 2, z + dz, Kind.SOLID)
        }
        g.set(x, BY - 1, z, k)
    }

    private fun buildMap(): Grid {
        val g = Grid()
        val beds = arrayOf(intArrayOf(0, -40), intArrayOf(40, 0), intArrayOf(0, 40), intArrayOf(-40, 0))
        for (b in beds) {
            g.set(b[0], BY, b[1], Kind.BED)
            g.set(b[0], BY, b[1] + 1, Kind.BED)
            g.set(b[0] + 3, BY - 1, b[1] - 4, Kind.SOLID)
            g.set(b[0] + 3, BY, b[1] - 4, Kind.DIAMOND_BLOCK) // diamond set into a wall beside the bed
            g.set(b[0] + 3, BY + 1, b[1] - 4, Kind.SOLID)
        }
        pedestal(g, 12, 0, Kind.DIAMOND_BLOCK)
        pedestal(g, -12, 0, Kind.DIAMOND_BLOCK)
        pedestal(g, 0, 12, Kind.DIAMOND_BLOCK)
        pedestal(g, 0, -12, Kind.DIAMOND_BLOCK)
        pedestal(g, 0, 0, Kind.EMERALD_BLOCK)
        flush(g, 16, 16, Kind.EMERALD_BLOCK)
        flush(g, -16, 16, Kind.EMERALD_BLOCK)
        flush(g, 16, -16, Kind.EMERALD_BLOCK)
        flush(g, -16, -16, Kind.EMERALD_BLOCK)
        for (x in 30..32) for (y in BY..BY + 2) for (z in 30..32) g.set(x, y, z, Kind.DIAMOND_BLOCK)
        g.set(30, BY - 1, 30, Kind.SOLID)
        flush(g, 25, -10, Kind.DIAMOND_BLOCK)
        return g
    }

    private fun accepted(r: GeneratorDetector.Result, t: Kind, at: DoubleArray): Boolean =
        r.candidates.any { it.type == t && it.p >= 0.6 && hypot(it.x - at[0], it.z - at[2]) < 0.6 && abs(it.y - at[1]) < 0.6 }

    private fun anyAccepted(r: GeneratorDetector.Result, x0: Double, x1: Double, z0: Double, z1: Double): Boolean =
        r.candidates.any { it.p >= 0.6 && it.x in x0..x1 && it.z in z0..z1 }

    @JvmStatic
    fun main(args: Array<String>) {
        val g = buildMap()
        val r = GeneratorDetector.detect(g, -100, 100, BY - 30, BY + 30, -100, 100, 0.0, 0.0)
        check(r.symmetric, "map recognised as symmetric")
        val dia = arrayOf(
            doubleArrayOf(12.5, BY + 1.0, 0.5), doubleArrayOf(-11.5, BY + 1.0, 0.5),
            doubleArrayOf(0.5, BY + 1.0, 12.5), doubleArrayOf(0.5, BY + 1.0, -11.5)
        )
        for (d in dia) check(accepted(r, Kind.DIAMOND_BLOCK, d), "diamond pedestal accepted at ${d[0]},${d[2]}")
        check(accepted(r, Kind.EMERALD_BLOCK, doubleArrayOf(0.5, BY + 1.0, 0.5)), "emerald pedestal at the centre accepted")
        val em = arrayOf(
            doubleArrayOf(16.5, BY.toDouble(), 16.5), doubleArrayOf(-15.5, BY.toDouble(), 16.5),
            doubleArrayOf(16.5, BY.toDouble(), -15.5), doubleArrayOf(-15.5, BY.toDouble(), -15.5)
        )
        for (e in em) check(accepted(r, Kind.EMERALD_BLOCK, e), "flush emerald accepted at ${e[0]},${e[2]}")
        check(!anyAccepted(r, 29.0, 33.0, 29.0, 33.0), "solid diamond cube rejected")
        check(!anyAccepted(r, 24.0, 26.0, -11.0, -9.0), "lone flush diamond with no partner rejected in a symmetric map")
        val n = r.candidates.count { it.p >= 0.6 }
        check(n == 9, "exactly 9 generators accepted (got $n)")
        println(if (failures == 0) "ALL PASS" else "$failures FAILURE(S)")
        exitProcess(if (failures == 0) 0 else 1)
    }
}
