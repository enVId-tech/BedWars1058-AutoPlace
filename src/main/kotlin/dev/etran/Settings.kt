package dev.etran

import dev.etran.model.Element
import dev.etran.model.Facing
import org.bukkit.configuration.file.FileConfiguration
import java.util.Locale

/** Typed access to config.yml. */
class Settings(private val plugin: EasySetupPlugin) {
    private fun c(): FileConfiguration = plugin.config

    fun bwRoot(): String = c().getString("bw-commands.root", "bw")!!

    fun commandTemplate(key: String): String = c().getString("bw-commands.$key", "")!!

    fun commandRoots(): List<String> {
        val out = c().getStringList("command-roots").map { it.lowercase(Locale.ROOT) }.toMutableList()
        if (out.isEmpty()) out.add("bw")
        return out
    }

    fun stepDelay(): Int = maxOf(1, c().getInt("timing.step-delay-ticks", 2))

    fun countdownSeconds(): Int = maxOf(1, c().getInt("timing.countdown-seconds", 3))

    fun centerX(): Double = c().getDouble("center.x", 0.0)

    fun centerZ(): Double = c().getDouble("center.z", 0.0)

    fun snap(): Boolean = c().getBoolean("snap-to-half-block", true)

    fun templateBedRadius(): Int = c().getInt("beds.template-search-radius", 30)

    fun bedScanRadius(): Int = c().getInt("beds.scan-radius", 200)

    fun bedArmorStandRadius(): Double = c().getDouble("beds.armor-stand-radius", 7.0)

    fun assignedBedRadius(): Double = c().getDouble("beds.assigned-radius", 10.0)

    fun lobbyRadius(): Int = maxOf(8, c().getInt("lobby-scan.radius", 48))

    fun lobbyMinBlocks(): Int = c().getInt("lobby-scan.min-blocks", 24)

    fun lobbyMaxOffset(): Double = c().getDouble("lobby-scan.max-center-offset", 32.0)

    fun lobbyPadding(): Int = maxOf(0, c().getInt("lobby-scan.padding", 1))

    fun woolRadius(): Int = maxOf(4, c().getInt("wool.radius", 24))

    fun woolMinScore(): Int = c().getInt("wool.min-score", 6)

    /** Block families that count as team-coloured structure: WOOL, CONCRETE, TERRACOTTA, GLASS, CARPET. */
    fun woolFamilies(): List<String> {
        val l = c().getStringList("wool.families")
        if (l.isNullOrEmpty()) return listOf("WOOL")
        return l.map { it.uppercase(Locale.ROOT) }
    }

    /** Vertical half-range of the bed scan around the map centre's reference height. */
    fun bedScanYRange(): Int = maxOf(8, c().getInt("beds.scan-y-range", 80))

    /** auto | off | force */
    fun mirrorMode(): String = c().getString("mirror.mode", "auto")!!.lowercase(Locale.ROOT)

    fun mirrorRadius(): Int = maxOf(4, c().getInt("mirror.sample-radius", 14))

    fun mirrorMargin(): Double = c().getDouble("mirror.margin", 0.03)

    fun generatorScanRadius(): Int = maxOf(8, c().getInt("generator-scan.radius", 100))

    /** Vertical half-range of the generator scan around the player. */
    fun generatorYRange(): Int = maxOf(8, c().getInt("generator-scan.y-range", 48))

    /** Candidates at or above this probability are treated as generators. */
    fun generatorMinProbability(): Double = c().getDouble("generator-scan.min-probability", 0.6)

    /** Shop/upgrade yaw snap in degrees; 0 turns snapping off. */
    fun shopSnapDegrees(): Double = maxOf(0.0, c().getDouble("facing.shop-snap-degrees", 45.0))

    /** toward-player | same | fixed */
    fun shopFacingMode(): Facing.Mode =
        Facing.Mode.parse(c().getString("facing.shop-mode", "toward-player")) ?: Facing.Mode.TOWARD_PLAYER

    fun shopFixedYaw(): Double = c().getDouble("facing.shop-fixed-yaw", 0.0)

    /** Pitch given to shops and upgrades (locked to 0 unless changed). */
    fun shopPitch(): Double = c().getDouble("facing.shop-pitch", 0.0)

    /** Spawn yaw snap in degrees (the spawn faces the way the player faces); 0 = exact. */
    fun spawnSnapDegrees(): Double = maxOf(0.0, c().getDouble("facing.spawn-snap-degrees", 45.0))

    /** BedWars1058 data folder holding Arenas/<world>.yml, for importing elements that already exist. */
    fun arenaFolder(): String = c().getString("existing-arena.folder", "Arenas")!!

    fun importExisting(): Boolean = c().getBoolean("existing-arena.import-on-start", true)

    fun ghostsEnabled(): Boolean = c().getBoolean("ghosts.enabled", true)

    fun deleteConfirmSeconds(): Double = maxOf(1.0, c().getDouble("delete.confirm-seconds", 5.0))

    fun defaultRotation(): Double = c().getDouble("rotation.default-degrees", 90.0)

    fun requiredMapElements(): List<Element> {
        val out = ArrayList<Element>()
        for (s in c().getStringList("validation.required-map-elements")) {
            try {
                val e = Element.valueOf(s.uppercase(Locale.ROOT))
                if (!e.perTeam) out.add(e)
            } catch (ignored: IllegalArgumentException) {
                plugin.logger.warning("Unknown element in validation.required-map-elements: $s")
            }
        }
        return out
    }

    fun incompleteAutoMessage(elements: String): String {
        val t = c().getString(
            "messages.incomplete-auto",
            "You are attempting to run automatic placement without all game elements. " +
                "You are missing [{elements}]. Run /bwauto confirm/deny to continue anyway."
        )!!
        return t.replace("{elements}", elements)
    }
}
