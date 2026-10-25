package dev.etran.scan;

import dev.etran.Settings;
import dev.etran.model.BedInfo;
import dev.etran.model.SetupSession;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds beds on the map. Beds are block entities, so this walks the tile entities of generated chunks
 * instead of every block in a 200 block radius like the Skript did.
 */
public final class BedScanner {
    private BedScanner() {
    }

    /** All beds within {@code radius} blocks (3D) of the centre. */
    public static List<BedInfo> scan(World w, Location center, int radius) {
        Map<String, BedInfo> out = new LinkedHashMap<>();
        int ccx = center.getBlockX() >> 4;
        int ccz = center.getBlockZ() >> 4;
        int cr = (radius >> 4) + 1;
        for (int cx = ccx - cr; cx <= ccx + cr; cx++) {
            for (int cz = ccz - cr; cz <= ccz + cr; cz++) {
                if (!w.isChunkGenerated(cx, cz)) continue;
                Chunk ch = w.getChunkAt(cx, cz);
                for (BlockState st : ch.getTileEntities()) {
                    if (!(st instanceof org.bukkit.block.Bed)) continue;
                    if (st.getLocation().distance(center) > radius) continue;
                    BedInfo bi = BedInfo.fromBlock(st.getBlock());
                    if (bi != null) out.putIfAbsent(bi.key(), bi);
                }
            }
        }
        return new ArrayList<>(out.values());
    }

    /** The bed nearest to {@code loc} within {@code radius}, or null. */
    public static BedInfo nearest(World w, Location loc, int radius) {
        BedInfo best = null;
        double bestD = Double.MAX_VALUE;
        for (BedInfo b : scan(w, loc, radius)) {
            double d = b.footCenter(w).distance(loc);
            if (d < bestD) {
                bestD = d;
                best = b;
            }
        }
        return best;
    }

    public static boolean armorStandNear(World w, Location at, double radius) {
        for (Entity e : w.getNearbyEntities(at, radius, radius, radius)) {
            if (e instanceof ArmorStand && e.getLocation().distance(at) <= radius) return true;
        }
        return false;
    }

    /**
     * Beds that still need a team: not recorded by this session, not near an assigned bed, and with no
     * armor stand within the configured radius (Bedwars1058 spawns armor stands at placed elements).
     * Sorted by angle around the map centre so assignment order is deterministic.
     */
    public static List<BedInfo> unassigned(SetupSession s, Settings st, World w, Location center) {
        List<BedInfo> out = new ArrayList<>();
        for (BedInfo b : scan(w, center, st.bedScanRadius())) {
            boolean skip = false;
            for (BedInfo a : s.assignedBeds) {
                if (a.key().equals(b.key()) || a.distanceTo(b) < st.assignedBedRadius()) {
                    skip = true;
                    break;
                }
            }
            if (skip) continue;
            if (armorStandNear(w, b.footCenter(w), st.bedArmorStandRadius())) continue;
            out.add(b);
        }
        double cx = st.centerX();
        double cz = st.centerZ();
        out.sort(Comparator.comparingDouble(b -> Math.atan2(b.z() + 0.5 - cz, b.x() + 0.5 - cx)));
        return out;
    }
}
