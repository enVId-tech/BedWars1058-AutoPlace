package dev.etran.model

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.util.EnumMap
import java.util.UUID

/** All in-memory state for one player's arena setup. */
class SetupSession(val owner: UUID) {
    var mode: SetupMode = SetupMode.SEMI
    var gamemode: GameModeType? = null
    val teams: MutableMap<TeamColor, TeamState> = EnumMap(TeamColor::class.java)
    val template = Template()
    val mapElements: MutableMap<Element, MutableList<Location>> = EnumMap(Element::class.java)
    val assignedBeds: MutableList<BedInfo> = ArrayList()
    val mirrorCache: MutableMap<String, Boolean> = HashMap()
    var selected: TeamColor? = null

    /** Semi-automatic mode: detect the lobby structure instead of placing the markers by hand. */
    var autoLobby = false
    var warnedMissingTeam = false
    var busy = false

    // ---- hotbar tool mode
    var toolsActive = false

    /** Existing arena-file elements were already loaded into this session. */
    var imported = false

    /** 0 = team tools, 1 = map tools. */
    var hotbarPage = 0

    /** The hotbar items the tools replaced; given back when tool mode ends. */
    var savedHotbar: Array<ItemStack?>? = null
    var savedHeldSlot = 0

    /** Per-session overrides of the facing config (null = use config.yml). */
    var facingMode: Facing.Mode? = null
    var facingFixed = 0.0
    var snapOverride: Double? = null
    var pitchOverride: Double? = null

    /** Two-hit delete: which element was hit first and until when the second hit counts. */
    var pendingDeleteKey: String? = null
    var pendingDeleteUntil = 0L

    var pendingAuto = false
    var pendingSingle = false
    var confirmedIncomplete = false
    var saveConfirmUntil = 0L

    fun team(c: TeamColor): TeamState = teams.getOrPut(c) { TeamState(c) }

    fun hasTeam(c: TeamColor): Boolean = teams[c]?.created == true

    fun createdTeams(): List<TeamColor> = TeamColor.entries.filter { hasTeam(it) }

    fun requiredTeams(): List<TeamColor> {
        val g = gamemode ?: return emptyList()
        return TeamColor.defaults(g.teams)
    }

    fun teamPlaced(c: TeamColor, e: Element): Boolean = teams[c]?.placed?.contains(e) == true

    fun mapPlaced(e: Element): Boolean = !mapElements[e].isNullOrEmpty()

    fun mapCount(e: Element): Int = mapElements[e]?.size ?: 0

    /** Single-position map element (spawns, region corners): replaces any previous position. */
    fun setMap(e: Element, loc: Location) {
        mapElements[e] = arrayListOf(loc.clone())
    }

    /** Generators accumulate. */
    fun addMapGen(e: Element, loc: Location) {
        mapElements.getOrPut(e) { ArrayList() }.add(loc.clone())
    }

    fun addAssigned(b: BedInfo) {
        if (assignedBeds.any { it.key() == b.key() }) return
        assignedBeds.add(b)
    }

    fun reset() {
        teams.clear()
        template.clear()
        mapElements.clear()
        assignedBeds.clear()
        mirrorCache.clear()
        selected = null
        warnedMissingTeam = false
        busy = false
        pendingAuto = false
        confirmedIncomplete = false
        saveConfirmUntil = 0L
        pendingDeleteKey = null
        pendingDeleteUntil = 0L
        imported = false
    }
}
