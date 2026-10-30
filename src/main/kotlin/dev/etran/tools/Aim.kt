package dev.etran.tools

import dev.etran.scan.Mats
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player

/** Works out which cell a click points into, from a clicked block face or by ray-casting the player's view. */
object Aim {
    class Hit internal constructor(
        /** The block that was clicked. */
        @JvmField val block: Block,
        /** The cell next to it that the click points into. */
        @JvmField val cell: Block
    ) {
        /** Feet position for something placed in the cell: block centre horizontally, cell floor vertically. */
        fun cellCenter(): Location = Location(cell.world, cell.x + 0.5, cell.y.toDouble(), cell.z + 0.5)
    }

    @JvmStatic
    fun fromBlock(clicked: Block?, face: BlockFace?): Hit? {
        if (clicked == null) return null
        return Hit(clicked, if (face == null) clicked.getRelative(BlockFace.UP) else clicked.getRelative(face))
    }

    /** First solid block along the player's line of sight, or null when nothing is within [max] blocks. */
    @JvmStatic
    fun raycast(p: Player, max: Double): Hit? {
        val eye = p.eyeLocation
        val w = eye.world
        val step = eye.direction.normalize().multiply(0.05)
        val cur = eye.clone()
        var prev = cur.block
        var d = 0.0
        while (d <= max) {
            cur.add(step)
            val b = w.getBlockAt(cur.blockX, cur.blockY, cur.blockZ)
            if (b != prev) {
                if (!Mats.passable(b)) return Hit(b, prev)
                prev = b
            }
            d += 0.05
        }
        return null
    }
}
