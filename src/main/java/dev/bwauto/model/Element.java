package dev.etran.model;

import java.util.ArrayList;
import java.util.List;

/** Everything that has to exist for an arena to be complete. */
public enum Element {
    // per-team
    SPAWN("Spawn", true),
    BED("Bed", true),
    SHOP("Shop", true),
    UPGRADE("Upgrade", true),
    GEN_IRON("Iron Generator", true),
    GEN_GOLD("Gold Generator", true),
    KILL_DROPS("Drop Area", true),
    // map-wide
    WAITING_SPAWN("Player (Waiting) Spawn", false),
    SPECTATOR_SPAWN("Spectator Spawn", false),
    LOBBY_POS1("Lobby Region Corner 1", false),
    LOBBY_POS2("Lobby Region Corner 2", false),
    GEN_DIAMOND("Diamond Generator", false),
    GEN_EMERALD("Emerald Generator", false);

    public final String label;
    public final boolean perTeam;

    Element(String label, boolean perTeam) {
        this.label = label;
        this.perTeam = perTeam;
    }

    /** Elements whose position is captured into the base template (the bed is the origin, so it is not). */
    public boolean capturable() {
        return perTeam && this != BED;
    }

    /** Generator type string used by the Bedwars command, or null. */
    public String genType() {
        return switch (this) {
            case GEN_IRON -> "iron";
            case GEN_GOLD -> "gold";
            case GEN_DIAMOND -> "diamond";
            case GEN_EMERALD -> "emerald";
            default -> null;
        };
    }

    public boolean isGenerator() {
        return genType() != null;
    }

    public static List<Element> teamElements() {
        List<Element> out = new ArrayList<>();
        for (Element e : values()) {
            if (e.perTeam) out.add(e);
        }
        return out;
    }

    public static List<Element> mapElements() {
        List<Element> out = new ArrayList<>();
        for (Element e : values()) {
            if (!e.perTeam) out.add(e);
        }
        return out;
    }

    public static String join(List<Element> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i).label);
        }
        return sb.toString();
    }
}
