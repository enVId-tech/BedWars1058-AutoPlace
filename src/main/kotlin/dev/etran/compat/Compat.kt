package dev.etran.compat

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import java.lang.reflect.Method

/**
 * Runtime version adapters. The plugin is compiled against the 1.8.8 API as Java 8 bytecode so it loads
 * everywhere; anything newer than 1.8 is reached by name or reflection here and degrades gracefully.
 */
object Compat {
    /** True on 1.13+ (colour is part of the material name); false on 1.8-1.12 (WOOL + data value). */
    @JvmField
    val FLATTENED: Boolean = Material.getMaterial("RED_WOOL") != null

    private val GET_MIN_HEIGHT: Method? = method(World::class.java, "getMinHeight")
    private val NO_SOUND: Sound? = sound("ENTITY_VILLAGER_NO", "VILLAGER_NO")

    private fun method(c: Class<*>, name: String, vararg args: Class<*>): Method? =
        try {
            c.getMethod(name, *args)
        } catch (t: Throwable) {
            null
        }

    /** Lowest block Y of the world: negative on 1.17+ worlds, 0 on older versions. */
    @JvmStatic
    fun minY(w: World): Int {
        val m = GET_MIN_HEIGHT
        if (m != null) {
            try {
                return m.invoke(w) as Int
            } catch (ignored: Throwable) {
                // fall through
            }
        }
        return 0
    }

    /** One past the highest block Y. */
    @JvmStatic
    fun maxY(w: World): Int = w.maxHeight

    /**
     * Looks a sound up by field name. Sound is an enum up to 1.21.2 and an interface after that, so
     * valueOf/values would break; reading the static field works for both.
     */
    @JvmStatic
    fun sound(vararg names: String): Sound? {
        for (n in names) {
            try {
                val o = Sound::class.java.getField(n).get(null)
                if (o is Sound) return o
            } catch (ignored: Throwable) {
                // try next
            }
        }
        return null
    }

    @JvmStatic
    fun playNo(p: Player) {
        val s = NO_SOUND ?: return
        try {
            p.playSound(p.location, s, 1f, 1f)
        } catch (ignored: Throwable) {
            // sound is cosmetic
        }
    }
}
