package dev.etran.compat

import dev.etran.model.BedInfo
import dev.etran.scan.Mats
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import java.lang.reflect.Method

/**
 * Reads a bed's orientation and head/foot part.
 * 1.13+: through the Bed BlockData (reflection, since the API is compiled against 1.8).
 * 1.8-1.12: from the block data value (bits 0-1 facing, bit 3 = head).
 */
object BedCompat {
    private val LEGACY_FACES = arrayOf(BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST)

    private val GET_BLOCK_DATA: Method?
    private val BED_CLASS: Class<*>?
    private val GET_FACING: Method?
    private val GET_PART: Method?

    init {
        var gbd: Method? = null
        var bc: Class<*>? = null
        var gf: Method? = null
        var gp: Method? = null
        try {
            gbd = Block::class.java.getMethod("getBlockData")
            bc = Class.forName("org.bukkit.block.data.type.Bed")
            gf = bc.getMethod("getFacing")
            gp = bc.getMethod("getPart")
        } catch (t: Throwable) {
            gbd = null
            bc = null
            gf = null
            gp = null
        }
        GET_BLOCK_DATA = gbd
        BED_CLASS = bc
        GET_FACING = gf
        GET_PART = gp
    }

    /** Resolve a bed from either of its two blocks, or null if the block is not a bed. */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun resolve(b: Block): BedInfo? {
        if (!Mats.isBed(b.type)) return null
        val facing: BlockFace
        val head: Boolean
        try {
            if (GET_BLOCK_DATA != null) {
                val d = GET_BLOCK_DATA.invoke(b)
                if (!BED_CLASS!!.isInstance(d)) return null
                facing = GET_FACING!!.invoke(d) as BlockFace
                head = "HEAD" == (GET_PART!!.invoke(d) as Enum<*>).name
            } else {
                val data = b.data.toInt() and 0xF
                facing = LEGACY_FACES[data and 3]
                head = (data and 8) != 0
            }
        } catch (t: Throwable) {
            return null
        }
        val foot = if (head) b.getRelative(facing.oppositeFace) else b
        if (!Mats.isBed(foot.type)) return null
        return BedInfo(b.world.name, foot.x, foot.y, foot.z, BedInfo.yawOf(facing), b.type.name)
    }
}
