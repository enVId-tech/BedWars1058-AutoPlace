package dev.etran.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** All in-memory state for one player's arena setup. */
public final class SetupSession {

    public final UUID owner;
    public SetupMode mode = SetupMode.SEMI;
    public GameModeType gamemode;
    public final Map<TeamColor, TeamState> teams = new EnumMap<>(TeamColor.class);
    public final Template template = new Template();
    public final Map<Element, List<Location>> mapElements = new EnumMap<>(Element.class);
    public final List<BedInfo> assignedBeds = new ArrayList<>();
    public final Map<String, Boolean> mirrorCache = new HashMap<>();

    public TeamColor selected;

    /** Semi-automatic mode: detect the lobby structure instead of placing the markers by hand. */
    public boolean autoLobby;
    public boolean warnedMissingTeam;
    public boolean busy;
    public boolean pendingAuto;
    public boolean pendingSingle;
    public boolean confirmedIncomplete;
    public long saveConfirmUntil;

    public SetupSession(UUID owner) {
        this.owner = owner;
    }

    public TeamState team(TeamColor c) {
        return teams.computeIfAbsent(c, TeamState::new);
    }

    public boolean hasTeam(TeamColor c) {
        TeamState t = teams.get(c);
        return t != null && t.created;
    }

    public List<TeamColor> createdTeams() {
        List<TeamColor> out = new ArrayList<>();
        for (TeamColor c : TeamColor.values()) {
            if (hasTeam(c)) out.add(c);
        }
        return out;
    }

    public List<TeamColor> requiredTeams() {
        return gamemode == null ? List.of() : TeamColor.defaults(gamemode.teams);
    }

    public boolean teamPlaced(TeamColor c, Element e) {
        TeamState t = teams.get(c);
        return t != null && t.placed.contains(e);
    }

    public boolean mapPlaced(Element e) {
        List<Location> l = mapElements.get(e);
        return l != null && !l.isEmpty();
    }

    public int mapCount(Element e) {
        List<Location> l = mapElements.get(e);
        return l == null ? 0 : l.size();
    }

    /** Single-position map element (spawns, region corners): replaces any previous position. */
    public void setMap(Element e, Location loc) {
        List<Location> l = new ArrayList<>();
        l.add(loc.clone());
        mapElements.put(e, l);
    }

    /** Generators accumulate. */
    public void addMapGen(Element e, Location loc) {
        mapElements.computeIfAbsent(e, k -> new ArrayList<>()).add(loc.clone());
    }

    public void addAssigned(BedInfo b) {
        for (BedInfo a : assignedBeds) {
            if (a.key().equals(b.key())) return;
        }
        assignedBeds.add(b);
    }

    public void reset() {
        teams.clear();
        template.clear();
        mapElements.clear();
        assignedBeds.clear();
        mirrorCache.clear();
        selected = null;
        warnedMissingTeam = false;
        busy = false;
        pendingAuto = false;
        confirmedIncomplete = false;
        saveConfirmUntil = 0L;
    }
}
