package dev.etran.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class TeamState {

    /** A generator this plugin placed, kept so it can be removed before being re-placed. */
    public record GenRecord(String type, Location loc) {
    }

    public final TeamColor color;
    public boolean created;
    public final EnumSet<Element> placed = EnumSet.noneOf(Element.class);
    public final List<GenRecord> gens = new ArrayList<>();
    public BedInfo bed;

    public TeamState(TeamColor color) {
        this.color = color;
    }
}
