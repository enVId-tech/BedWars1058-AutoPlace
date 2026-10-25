package dev.etran.place;

import dev.etran.Settings;
import dev.etran.model.Element;
import dev.etran.model.SetupSession;
import dev.etran.model.TeamColor;

import java.util.ArrayList;
import java.util.List;

/** Answers "what is still missing?" for the warnings, the UI guard and the final validation screen. */
public final class Validator {
    private Validator() {
    }

    public static List<Element> missingForTeam(SetupSession s, TeamColor c) {
        List<Element> out = new ArrayList<>();
        for (Element e : Element.teamElements()) {
            if (!s.teamPlaced(c, e)) out.add(e);
        }
        return out;
    }

    public static List<Element> missingMap(SetupSession s, Settings st) {
        List<Element> out = new ArrayList<>();
        for (Element e : st.requiredMapElements()) {
            if (!s.mapPlaced(e)) out.add(e);
        }
        return out;
    }

    /**
     * Elements the automatic placement would copy from the template base but that were never placed on it.
     * The template team is the team that owns the captured deltas (or the selected / first team).
     */
    public static List<Element> missingTemplate(SetupSession s) {
        TeamColor t = s.template.team();
        if (t == null) t = s.selected;
        if (t == null && !s.requiredTeams().isEmpty()) t = s.requiredTeams().get(0);
        if (t == null) return Element.teamElements();
        return missingForTeam(s, t);
    }

    /** One human-readable line per problem, empty when the arena is complete. */
    public static List<String> missingAll(SetupSession s, Settings st) {
        List<String> lines = new ArrayList<>();
        if (s.gamemode == null) lines.add("No gamemode selected");
        for (TeamColor c : s.requiredTeams()) {
            if (!s.hasTeam(c)) {
                lines.add(c.display + ": team not created");
                continue;
            }
            List<Element> m = missingForTeam(s, c);
            if (!m.isEmpty()) lines.add(c.display + ": " + Element.join(m));
        }
        List<Element> mm = missingMap(s, st);
        if (!mm.isEmpty()) lines.add("Map: " + Element.join(mm));
        return lines;
    }

    public static boolean complete(SetupSession s, Settings st) {
        return missingAll(s, st).isEmpty();
    }
}
