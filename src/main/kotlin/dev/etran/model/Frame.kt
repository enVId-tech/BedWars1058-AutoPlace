package dev.etran.model

import kotlin.math.cos
import kotlin.math.sin

/**
 * A local coordinate frame anchored on a bed. Using the bed's facing as the frame orientation means a
 * base rotated by any angle (90/180/270 for symmetric maps, or arbitrary) maps onto another base by
 * simply re-anchoring the same local offsets - no per-angle lookup tables.
 *
 * Minecraft yaw: 0 = +Z (south), 90 = -X (west), 180 = -Z, 270 = +X.
 * Forward = (-sin, cos); right = (-cos, -sin).
 *
 * `mirror` reflects the base across its own forward axis (the "right" component and the
 * relative yaw are negated). This is the Skript's axis-specific mirroring, expressed in the base frame.
 */
class Frame(
    @JvmField val ox: Double,
    @JvmField val oy: Double,
    @JvmField val oz: Double,
    @JvmField val yaw: Double,
    @JvmField val mirror: Boolean
) {
    private val sin: Double = sin(Math.toRadians(yaw))
    private val cos: Double = cos(Math.toRadians(yaw))

    /** World position + yaw -> local offset (the mirror flag is ignored when capturing). */
    fun toLocal(x: Double, y: Double, z: Double, elementYaw: Double): Offset {
        val dx = x - ox
        val dz = z - oz
        val fwd = dx * -sin + dz * cos
        val right = dx * -cos + dz * -sin
        return Offset(right, y - oy, fwd, wrap180(elementYaw - yaw))
    }

    /** Local offset -> {x, y, z, yaw} in the world, honouring the mirror flag. */
    fun toWorld(o: Offset): DoubleArray {
        val r = if (mirror) -o.right else o.right
        val x = ox + r * -cos + o.fwd * -sin
        val z = oz + r * -sin + o.fwd * cos
        val y = oy + o.up
        val outYaw = if (mirror) yaw - o.dyaw else yaw + o.dyaw
        return doubleArrayOf(x, y, z, norm360(outYaw))
    }

    companion object {
        @JvmStatic
        fun norm360(a: Double): Double {
            val r = a % 360.0
            return if (r < 0) r + 360.0 else r
        }

        /** Nearest multiple of 45 degrees, in [0, 360). */
        @JvmStatic
        fun snap45(yaw: Double): Double = norm360(Math.round(norm360(yaw) / 45.0) * 45.0.toDouble())

        /** True for the elements whose facing is locked to 45 degree steps with pitch 0. */
        @JvmStatic
        fun locksFacing(e: Element): Boolean = e == Element.SHOP || e == Element.UPGRADE

        @JvmStatic
        fun wrap180(a: Double): Double {
            val r = norm360(a)
            return if (r > 180.0) r - 360.0 else r
        }
    }
}
