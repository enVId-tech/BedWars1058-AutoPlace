package dev.etran.place

import dev.etran.Settings
import dev.etran.model.Element
import dev.etran.model.SetupSession
import dev.etran.model.TeamColor

/** Answers "what is still missing?" for the warnings, the UI guard and the final validation screen. */
object Validator {
    @JvmStatic
    fun missingForTeam(s: SetupSession, c: TeamColor): List<Element> =
        Element.teamElements().filter { !s.teamPlaced(c, it) }

    @JvmStatic
    fun missingMap(s: SetupSession, st: Settings): List<Element> =
        st.requiredMapElements().filter { !s.mapPlaced(it) }

    @JvmStatic
    fun missingTemplate(s: SetupSession): List<Element> {
        val t = s.template.team ?: s.selected ?: s.requiredTeams().firstOrNull() ?: return Element.teamElements()
        return missingForTeam(s, t)
    }

    /** One human-readable line per problem, empty when the arena is complete. */
    @JvmStatic
    fun missingAll(s: SetupSession, st: Settings): List<String> {
        val lines = ArrayList<String>()
        if (s.gamemode == null) lines.add("No gamemode selected")
        for (c in s.requiredTeams()) {
            if (!s.hasTeam(c)) {
                lines.add(c.display + ": team not created")
                continue
            }
            val m = missingForTeam(s, c)
            if (m.isNotEmpty()) lines.add(c.display + ": " + Element.join(m))
        }
        val mm = missingMap(s, st)
        if (mm.isNotEmpty()) lines.add("Map: " + Element.join(mm))
        return lines
    }

    @JvmStatic
    fun complete(s: SetupSession, st: Settings): Boolean = missingAll(s, st).isEmpty()
}
