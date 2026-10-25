package dev.etran.scan;

import dev.etran.Settings;
import dev.etran.model.Element;
import dev.etran.model.SetupSession;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Port of the Skript's diamond/emerald detection: a diamond (or emerald) block with solid ground under
 * it, three air blocks above it, stairs on all four sides of the air block above it, and no armor stand
 * (already placed generator) within 1.5 blocks.
 */
public final class GeneratorScanner {
    private GeneratorScanner() {
    }

    public static List<Location> findUnplaced(World w, Location around, String type, Settings st,
                                              SetupSession s) {
        Material target = type.equals("diamond") ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK;
        Element el = type.equals("diamond") ? Element.GEN_DIAMOND : Element.GEN_EMERALD;
        int r = st.generatorScanRadius();
        int cx = (int) Math.floor(st.centerX());
        int cz = (int) Math.floor(st.centerZ());
        int ay = around.getBlockY();
        WorldSnap snap = new WorldSnap(w, cx - r, cz - r, cx + r, cz + r);
        int y0 = Math.max(snap.minY + 1, ay - r);
        int y1 = Math.min(snap.maxY - 5, ay + r);

        List<Location> out = new ArrayList<>();
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (snap.type(x, y, z) != target) continue;
                    if (snap.type(x, y - 1, z).isAir()) continue;
                    if (!snap.type(x, y + 1, z).isAir() || !snap.type(x, y + 2, z).isAir()
                            || !snap.type(x, y + 3, z).isAir()) continue;
                    if (!Mats.isStairs(snap.type(x, y + 1, z - 1)) || !Mats.isStairs(snap.type(x, y + 1, z + 1))
                            || !Mats.isStairs(snap.type(x + 1, y + 1, z))
                            || !Mats.isStairs(snap.type(x - 1, y + 1, z))) continue;
                    Location spot = new Location(w, x + 0.5, y + 1, z + 0.5);
                    if (armorStandNear(w, spot, 1.5) || alreadyRecorded(s, el, spot)) continue;
                    out.add(spot);
                }
            }
        }
        out.sort(Comparator.comparingDouble(l -> l.distanceSquared(around)));
        return out;
    }

    private static boolean armorStandNear(World w, Location at, double radius) {
        for (Entity e : w.getNearbyEntities(at, radius, radius, radius)) {
            if (e instanceof ArmorStand && e.getLocation().distance(at) <= radius) return true;
        }
        return false;
    }

    private static boolean alreadyRecorded(SetupSession s, Element el, Location spot) {
        List<Location> l = s.mapElements.get(el);
        if (l == null) return false;
        for (Location r : l) {
            if (r.getWorld() != null && r.getWorld().equals(spot.getWorld()) && r.distance(spot) < 1.5) return true;
        }
        return false;
    }
}
