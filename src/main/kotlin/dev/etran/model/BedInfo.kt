package dev.etran.model

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.BlockFace
import kotlin.math.sqrt

/**
 * A bed identified by its foot block. `facingYaw` is the yaw of the foot -> head direction, which
 * is the frame orientation for the base. Resolved from the world by `BedCompat`.
 */
class BedInfo(
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val facingYaw: Float,
    val material: String
) {
    fun key(): String = "$world,$x,$y,$z"

    fun footCenter(w: World): Location = Location(w, x + 0.5, y.toDouble(), z + 0.5)

    fun distanceTo(o: BedInfo): Double {
        val dx = (x - o.x).toDouble()
        val dy = (y - o.y).toDouble()
        val dz = (z - o.z).toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    companion object {
        @JvmStatic
        fun yawOf(f: BlockFace): Float = when (f) {
            BlockFace.WEST -> 90f
            BlockFace.NORTH -> 180f
            BlockFace.EAST -> 270f
            else -> 0f
        }
    }
}
