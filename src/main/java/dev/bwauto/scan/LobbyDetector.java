package dev.etran.scan;

import dev.etran.Settings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.BitSet;

/**
 * Detects the waiting-lobby structure around the map centre (x=0, z=0 by default - maps are symmetric
 * around it) and returns a rectangular bounding box around it.
 *
 * <p>Method: snapshot a square around the centre, mark every non-air block, flood-fill (26-neighbour)
 * into connected structures, discard structures containing beds (those are base islands) or smaller than
 * {@code lobby-scan.min-blocks}, and keep the one whose bounding-box centre is closest to the map centre.
 */
public final class LobbyDetector {
    private LobbyDetector() {
    }

    public record Result(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int blocks) {
        /** Exact centre of the box in X/Z (block space), at a standable height nearest the box's vertical middle. */
        public Location center() {
            double cx = (minX + maxX + 1) / 2.0;
            double cz = (minZ + maxZ + 1) / 2.0;
            int bx = (int) Math.floor(cx);
            int bz = (int) Math.floor(cz);
            int mid = (minY + maxY) / 2;
            int span = maxY - minY;
            for (int d = 0; d <= span; d++) {
                if (standable(world, bx, mid - d, bz)) return new Location(world, cx, mid - d, cz);
                if (d > 0 && standable(world, bx, mid + d, bz)) return new Location(world, cx, mid + d, cz);
            }
            return new Location(world, cx, mid, cz);
        }

        public Location corner1() {
            return new Location(world, minX + 0.5, minY, minZ + 0.5);
        }

        public Location corner2() {
            return new Location(world, maxX + 0.5, maxY, maxZ + 0.5);
        }
    }

    public static Result detect(World w, Settings st) {
        int r = st.lobbyRadius();
        int cx = (int) Math.floor(st.centerX());
        int cz = (int) Math.floor(st.centerZ());
        int minX = cx - r, maxX = cx + r, minZ = cz - r, maxZ = cz + r;
        WorldSnap snap = new WorldSnap(w, minX, minZ, maxX, maxZ);
        int minY = w.getMinHeight();
        int maxY = w.getMaxHeight();
        int width = 2 * r + 1;
        int height = maxY - minY;
        long total = (long) width * width * height;
        if (total > 60_000_000L) return null;
        int n = (int) total;

        BitSet solid = new BitSet(n);
        BitSet bed = new BitSet(n);
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                for (int y = 0; y < height; y++) {
                    Material m = snap.type(minX + x, minY + y, minZ + z);
                    if (m.isAir()) continue;
                    int idx = (x * width + z) * height + y;
                    solid.set(idx);
                    if (Mats.isBed(m)) bed.set(idx);
                }
            }
        }

        BitSet seen = new BitSet(n);
        int[] queue = new int[n];
        double bestDist = Double.MAX_VALUE;
        int[] best = null;
        int bestCount = 0;

        for (int i = solid.nextSetBit(0); i >= 0; i = solid.nextSetBit(i + 1)) {
            if (seen.get(i)) continue;
            int head = 0;
            int tail = 0;
            queue[tail++] = i;
            seen.set(i);
            int count = 0;
            boolean hasBed = false;
            int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
            int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
            while (head < tail) {
                int c = queue[head++];
                count++;
                int y = c % height;
                int t = c / height;
                int z = t % width;
                int x = t / width;
                x0 = Math.min(x0, x); x1 = Math.max(x1, x);
                y0 = Math.min(y0, y); y1 = Math.max(y1, y);
                z0 = Math.min(z0, z); z1 = Math.max(z1, z);
                if (bed.get(c)) hasBed = true;
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    if (nx < 0 || nx >= width) continue;
                    for (int dz = -1; dz <= 1; dz++) {
                        int nz = z + dz;
                        if (nz < 0 || nz >= width) continue;
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            int ny = y + dy;
                            if (ny < 0 || ny >= height) continue;
                            int ni = (nx * width + nz) * height + ny;
                            if (solid.get(ni) && !seen.get(ni)) {
                                seen.set(ni);
                                queue[tail++] = ni;
                            }
                        }
                    }
                }
            }
            if (hasBed || count < st.lobbyMinBlocks()) continue;
            double midX = minX + (x0 + x1 + 1) / 2.0;
            double midZ = minZ + (z0 + z1 + 1) / 2.0;
            double dist = Math.hypot(midX - st.centerX(), midZ - st.centerZ());
            if (dist > st.lobbyMaxOffset()) continue;
            if (best == null || dist < bestDist - 1e-6 || (Math.abs(dist - bestDist) <= 1e-6 && count > bestCount)) {
                bestDist = dist;
                bestCount = count;
                best = new int[]{x0, y0, z0, x1, y1, z1};
            }
        }
        if (best == null) return null;
        int pad = st.lobbyPadding();
        return new Result(w,
                minX + best[0] - pad, Math.max(minY, minY + best[1] - pad), minZ + best[2] - pad,
                minX + best[3] + pad, Math.min(maxY - 1, minY + best[4] + pad), minZ + best[5] + pad,
                bestCount);
    }

    static boolean standable(World w, int x, int y, int z) {
        if (y <= w.getMinHeight() || y >= w.getMaxHeight() - 1) return false;
        return w.getBlockAt(x, y - 1, z).getType().isSolid()
                && w.getBlockAt(x, y, z).isPassable()
                && w.getBlockAt(x, y + 1, z).isPassable();
    }
}
