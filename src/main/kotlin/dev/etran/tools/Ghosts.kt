package dev.etran.tools

import dev.etran.EasySetupPlugin
import dev.etran.model.Element
import dev.etran.model.SetupSession
import dev.etran.model.TeamColor
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * Ghost villagers: real entities that mark where shops and upgrades are (looking the way they face) while a
 * player is in setup mode. They are removed when the player leaves or exits, when a world is about to save, when
 * their chunk unloads and on disable, and any left behind are swept up on chunk load, so none reaches a real round.
 */
class Ghosts(private val plugin: EasySetupPlugin) {
    internal class Entry(val owner: UUID, val element: Element, val team: TeamColor?, val anchor: Location) {
        var entity: Entity? = null
    }

    private val byOwner = HashMap<UUID, MutableList<Entry>>()
    private val byEntity = HashMap<UUID, Entry>()
    private var keeper: BukkitTask? = null
    private var rebuildQueued = false

    fun start() {
        keeper = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { keep() }, 20L, 10L)
        // Remove ghosts a crash may have left in loaded chunks.
        for (w in Bukkit.getWorlds()) sweep(w)
    }

    fun stop() {
        keeper?.cancel()
        removeAll()
    }

    // ------------------------------------------------------------------------------------------

    /** Rebuild the owner's ghosts from the positions recorded in the session. */
    fun refresh(p: Player, s: SetupSession) {
        removeOwner(p.uniqueId)
        if (!plugin.settings().ghostsEnabled() || !s.toolsActive) return
        for ((color, ts) in s.teams) {
            for (e in arrayOf(Element.SHOP, Element.UPGRADE)) {
                val l = ts.pos[e]
                if (l != null && l.world != null) spawn(p.uniqueId, e, color, l)
            }
        }
    }

    private fun spawn(owner: UUID, e: Element, team: TeamColor?, l: Location) {
        val en = Entry(owner, e, team, l.clone())
        byOwner.getOrPut(owner) { ArrayList() }.add(en)
        materialise(en)
    }

    private fun materialise(en: Entry) {
        val l = en.anchor
        val w = l.world
        if (w == null || !w.isChunkLoaded(l.blockX shr 4, l.blockZ shr 4)) return
        try {
            val v = w.spawnEntity(l, EntityType.VILLAGER) as Villager
            val label = if (en.element == Element.SHOP) "Shop" else "Upgrades"
            v.customName = TAG + (en.team?.chat ?: ChatColor.WHITE) + label + ChatColor.GRAY + " (ghost)"
            v.isCustomNameVisible = true
            v.removeWhenFarAway = false
            try {
                v.profession = if (en.element == Element.SHOP) Villager.Profession.FARMER else Villager.Profession.LIBRARIAN
            } catch (ignored: Throwable) {
                // profession is cosmetic
            }
            val ai = call(v, "setAI", arrayOf(java.lang.Boolean.TYPE), false)
            call(v, "setSilent", arrayOf(java.lang.Boolean.TYPE), true)
            call(v, "setInvulnerable", arrayOf(java.lang.Boolean.TYPE), true)
            call(v, "setCollidable", arrayOf(java.lang.Boolean.TYPE), false)
            call(v, "setPersistent", arrayOf(java.lang.Boolean.TYPE), false)
            if (!ai) immobilise(v) // 1.8: no AI switch
            v.teleport(l)
            call(v, "setRotation", arrayOf(java.lang.Float.TYPE, java.lang.Float.TYPE), l.yaw, l.pitch)
            en.entity = v
            byEntity[v.uniqueId] = en
        } catch (t: Throwable) {
            plugin.logger.warning("Could not spawn a ghost villager: $t")
        }
    }

    private fun immobilise(v: Villager) {
        try {
            PotionEffectType.getByName("SLOW")?.let { v.addPotionEffect(PotionEffect(it, Int.MAX_VALUE, 255, true)) }
            PotionEffectType.getByName("JUMP")?.let { v.addPotionEffect(PotionEffect(it, Int.MAX_VALUE, 128, true)) }
        } catch (ignored: Throwable) {
            // the keeper task still pulls it back
        }
    }

    private fun call(target: Any, name: String, types: Array<Class<*>>, vararg args: Any): Boolean = try {
        target.javaClass.getMethod(name, *types).invoke(target, *args)
        true
    } catch (t: Throwable) {
        false
    }

    // ------------------------------------------------------------------------------------------

    fun removeOwner(owner: UUID) {
        val list = byOwner.remove(owner) ?: return
        for (en in list) dispose(en)
    }

    fun removeAll() {
        for (list in ArrayList(byOwner.values)) for (en in list) dispose(en)
        byOwner.clear()
        byEntity.clear()
    }

    private fun dispose(en: Entry) {
        val e = en.entity ?: return
        byEntity.remove(e.uniqueId)
        try {
            e.remove()
        } catch (ignored: Throwable) {
            // already gone
        }
        en.entity = null
    }

    /** Remove every ghost now (a world is about to be saved) and bring them back a moment later. */
    fun removeForSave() {
        val any = byOwner.isNotEmpty()
        for (list in byOwner.values) for (en in list) dispose(en)
        // Entries stay; they are respawned from their anchors below.
        if (rebuildQueued || !any) return
        rebuildQueued = true
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            rebuildQueued = false
            for (list in byOwner.values) for (en in list) if (en.entity == null) materialise(en)
        }, 10L)
    }

    fun isGhost(e: Entity?): Boolean = e != null && (byEntity.containsKey(e.uniqueId) || tagged(e))

    /** The recorded element behind a ghost entity, or null. */
    internal fun entryOf(e: Entity?): Entry? = if (e == null) null else byEntity[e.uniqueId]

    private fun tagged(e: Entity): Boolean {
        if (e !is Villager) return false
        val n = e.customName
        return n != null && n.startsWith(TAG)
    }

    // ---- leftovers ---------------------------------------------------------------------------

    fun sweep(w: World) {
        for (c in w.loadedChunks) sweepEntities(c.entities.toList())
    }

    fun sweepEntities(entities: Collection<Entity>) {
        for (e in ArrayList(entities)) {
            if (tagged(e) && !byEntity.containsKey(e.uniqueId)) e.remove()
        }
    }

    /** Remove ghosts in a chunk that is about to unload (their entries respawn them if it loads again). */
    fun chunkUnloading(c: Chunk) {
        for (list in byOwner.values) {
            for (en in list) {
                val e = en.entity
                if (e != null && e.location.chunk == c) dispose(en)
            }
        }
        sweepEntities(c.entities.toList())
    }

    private fun keep() {
        for (list in byOwner.values) {
            for (en in list) {
                val e = en.entity
                if (e == null || !e.isValid) {
                    if (e != null) byEntity.remove(e.uniqueId)
                    en.entity = null
                    materialise(en)
                } else if (e.location.distanceSquared(en.anchor) > 0.09) {
                    e.teleport(en.anchor)
                }
            }
        }
    }

    companion object {
        /** Invisible prefix on every ghost's name; how leftovers are recognised. */
        internal const val TAG = "§r§b§w§a§g"
    }
}
