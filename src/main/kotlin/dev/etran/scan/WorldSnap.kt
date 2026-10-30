package dev.etran.scan

import dev.etran.compat.Compat
import org.bukkit.ChunkSnapshot
import org.bukkit.Material
import org.bukkit.World
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * Fast read-only block access over a rectangle of chunks that exist on disk or are loaded.
 *
 * ChunkSnapshot#getBlockType exists from 1.13 on and returns a Material; before that only
 * getBlockTypeId (a numeric id) exists. Both paths are handled here, the modern one through a method handle
 * so it runs at near-native speed. Nothing is generated: missing chunks read as air.
 */
class WorldSnap(w: World, minX: Int, minZ: Int, maxX: Int, maxZ: Int) {
    private val chunks = HashMap<Long, ChunkSnapshot>()
    @JvmField
    val minY: Int = Compat.minY(w)

    /** Exclusive. */
    @JvmField
    val maxY: Int = Compat.maxY(w)

    init {
        for (cx in (minX shr 4)..(maxX shr 4)) {
            for (cz in (minZ shr 4)..(maxZ shr 4)) {
                val s = snapshot(w, cx, cz)
                if (s != null) chunks[key(cx, cz)] = s
            }
        }
    }

    fun type(x: Int, y: Int, z: Int): Material {
        if (y < minY || y >= maxY) return Material.AIR
        val s = chunks[key(x shr 4, z shr 4)] ?: return Material.AIR
        return type(s, x and 15, y, z and 15)
    }

    fun legacyData(x: Int, y: Int, z: Int): Int {
        if (y < minY || y >= maxY) return 0
        val s = chunks[key(x shr 4, z shr 4)] ?: return 0
        return legacyData(s, x and 15, y, z and 15)
    }

    companion object {
        private val GET_TYPE: MethodHandle? = bind()
        private val BY_ID = arrayOfNulls<Material>(4096)

        private fun bind(): MethodHandle? = try {
            MethodHandles.publicLookup().findVirtual(
                ChunkSnapshot::class.java, "getBlockType",
                MethodType.methodType(Material::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            )
        } catch (t: Throwable) {
            null // pre-1.13
        }

        /** Snapshot of an existing chunk (loading it from disk if needed, never generating), or null. */
        @JvmStatic
        fun snapshot(w: World, cx: Int, cz: Int): ChunkSnapshot? {
            if (!w.isChunkLoaded(cx, cz) && !w.loadChunk(cx, cz, false)) return null
            return w.getChunkAt(cx, cz).getChunkSnapshot(false, false, false)
        }

        /** Block type at chunk-local x/z and absolute y. */
        @JvmStatic
        @Suppress("DEPRECATION")
        fun type(s: ChunkSnapshot, lx: Int, y: Int, lz: Int): Material {
            return try {
                val h = GET_TYPE
                if (h != null) {
                    h.invoke(s, lx, y, lz) as Material
                } else {
                    val id = s.getBlockTypeId(lx, y, lz) and 0xFFF
                    var m = BY_ID[id]
                    if (m == null) {
                        m = Material.getMaterial(id) ?: Material.AIR
                        BY_ID[id] = m
                    }
                    m
                }
            } catch (t: Throwable) {
                Material.AIR
            }
        }

        /** Legacy data value (0-15) at chunk-local x/z; always 0 on 1.13+ where it no longer exists. */
        @JvmStatic
        @Suppress("DEPRECATION")
        fun legacyData(s: ChunkSnapshot, lx: Int, y: Int, lz: Int): Int {
            if (Compat.FLATTENED) return 0
            return try {
                s.getBlockData(lx, y, lz)
            } catch (t: Throwable) {
                0
            }
        }

        private fun key(cx: Int, cz: Int): Long = (cx.toLong() shl 32) xor (cz.toLong() and 0xffffffffL)
    }
}
