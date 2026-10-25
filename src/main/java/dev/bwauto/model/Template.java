package dev.etran.model;

import org.bukkit.Location;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The first base the user sets up. Raw world positions are stored as captured; deltas (position and
 * rotation) are always derived on demand relative to the origin bed, so the bed can be resolved after the
 * other elements were captured.
 */
public final class Template {

    public record Raw(double x, double y, double z, double yaw) {
    }

    private final Map<Element, Raw> raw = new EnumMap<>(Element.class);
    private TeamColor team;
    private BedInfo bed;

    public TeamColor team() {
        return team;
    }

    public void setTeam(TeamColor team) {
        this.team = team;
    }

    public BedInfo bed() {
        return bed;
    }

    public void setBed(BedInfo bed) {
        this.bed = bed;
    }

    public boolean captured(Element e) {
        return raw.containsKey(e);
    }

    public Map<Element, Raw> rawView() {
        return Collections.unmodifiableMap(raw);
    }

    /** Capture a point: x/y/z snapped to 0.5, yaw snapped to 45 degrees for the shop and 90 for the rest. */
    public void capture(Element e, Location loc, boolean snap) {
        if (!e.capturable()) return;
        double x = snap ? half(loc.getX()) : loc.getX();
        double y = snap ? half(loc.getY()) : loc.getY();
        double z = snap ? half(loc.getZ()) : loc.getZ();
        double step = e == Element.SHOP ? 45.0 : 90.0;
        double yaw = Frame.norm360(Math.round(Frame.norm360(loc.getYaw()) / step) * step);
        raw.put(e, new Raw(x, y, z, yaw));
    }

    public Frame frame() {
        if (bed == null) return null;
        return new Frame(bed.x() + 0.5, bed.y(), bed.z() + 0.5, bed.facingYaw(), false);
    }

    /** Offset of the element from the origin bed, or null if the element or the bed is not known yet. */
    public Offset offset(Element e) {
        Raw r = raw.get(e);
        Frame f = frame();
        if (r == null || f == null) return null;
        return f.toLocal(r.x(), r.y(), r.z(), r.yaw());
    }

    public void clear() {
        raw.clear();
        bed = null;
        team = null;
    }

    private static double half(double v) {
        return Math.round(v * 2.0) / 2.0;
    }
}
