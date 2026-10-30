package dev.etran.model

import org.bukkit.Location
import java.util.EnumMap
import java.util.EnumSet

class TeamState(val color: TeamColor) {
    /** A generator this plugin placed, kept so it can be removed before being re-placed. */
    class GenRecord(val type: String, val loc: Location)

    var created: Boolean = false
    val placed: EnumSet<Element> = EnumSet.noneOf(Element::class.java)
    val gens: MutableList<GenRecord> = ArrayList()
    var bed: BedInfo? = null

    /** Where each single-position element (spawn, shop, upgrade, drop area, bed) was placed, with its facing. */
    val pos: MutableMap<Element, Location> = EnumMap(Element::class.java)
}
