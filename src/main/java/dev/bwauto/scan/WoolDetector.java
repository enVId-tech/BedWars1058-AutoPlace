package dev.etran.scan;

import dev.etran.Settings;
import dev.etran.model.BedInfo;
import dev.etran.model.TeamColor;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out which team colour each base is by isolating its wool from structural wool shared by every
 * base.
 *
 * <ol>
 *   <li>Every wool block near a bed is counted towards the <em>nearest</em> bed only, so neighbouring
 *       bases do not leak into each other.</li>
 *   <li>A colour's score at a base is (its count there) minus (its mean count at the other bases). A
 *       colour used equally everywhere (white wool in the shared walls, say) scores about zero; the team
 *       colour stands out.</li>
 *   <li>Colours are assigned to bases so each colour is used once and the total score is maximal
 *       (exhaustive search for up to 6 bases, greedy beyond that).</li>
 * </ol>
 */
public final class WoolDetector {
    private WoolDetector() {
    }

    public record Result(Map<String, TeamColor> byBed, Map<String, int[]> counts) {
    }

    public static Result detect(World w, List<BedInfo> beds, Settings st, Collection<TeamColor> allowed) {
        int n = beds.size();
        TeamColor[] cols = TeamColor.values();
        int[][] counts = new int[n][cols.length];
        Map<String, TeamColor> out = new HashMap<>();
        Map<String, int[]> countsByBed = new HashMap<>();
        if (n == 0) return new Result(out, countsByBed);

        int radius = st.woolRadius();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BedInfo b : beds) {
            minX = Math.min(minX, b.x()); maxX = Math.max(maxX, b.x());
            minZ = Math.min(minZ, b.z()); maxZ = Math.max(maxZ, b.z());
        }
        WorldSnap snap = new WorldSnap(w, minX - radius, minZ - radius, maxX + radius, maxZ + radius);
        List<String> suffixes = st.woolSuffixes();

        for (int i = 0; i < n; i++) {
            BedInfo b = beds.get(i);
            for (int x = b.x() - radius; x <= b.x() + radius; x++) {
                for (int z = b.z() - radius; z <= b.z() + radius; z++) {
                    if (nearestBed(beds, x, z) != i) continue;
                    for (int y = b.y() - 12; y <= b.y() + 16; y++) {
                        Material m = snap.type(x, y, z);
                        for (String suf : suffixes) {
                            String dye = Mats.dyeOf(m, suf);
                            if (dye == null) continue;
                            TeamColor c = TeamColor.fromDye(dye);
                            if (c != null) counts[i][c.ordinal()]++;
                            break;
                        }
                    }
                }
            }
        }

        double[][] score = new double[n][cols.length];
        for (int i = 0; i < n; i++) {
            for (int c = 0; c < cols.length; c++) {
                double others = 0;
                for (int j = 0; j < n; j++) {
                    if (j != i) others += counts[j][c];
                }
                double mean = n > 1 ? others / (n - 1) : 0;
                score[i][c] = counts[i][c] - mean;
            }
        }

        boolean[] ok = new boolean[cols.length];
        for (int c = 0; c < cols.length; c++) ok[c] = allowed.isEmpty() || allowed.contains(cols[c]);

        int[] assign = new int[n];
        java.util.Arrays.fill(assign, -1);
        if (n <= 6) {
            int[] best = new int[n];
            java.util.Arrays.fill(best, -1);
            double[] bestVal = {Double.NEGATIVE_INFINITY};
            search(0, 0, 0.0, new int[n], score, ok, st.woolMinScore(), best, bestVal);
            assign = best;
        } else {
            boolean[] used = new boolean[cols.length];
            for (int i = 0; i < n; i++) {
                int pick = -1;
                double pv = st.woolMinScore();
                for (int c = 0; c < cols.length; c++) {
                    if (ok[c] && !used[c] && score[i][c] >= pv) {
                        pv = score[i][c];
                        pick = c;
                    }
                }
                if (pick >= 0) {
                    used[pick] = true;
                    assign[i] = pick;
                }
            }
        }

        for (int i = 0; i < n; i++) {
            countsByBed.put(beds.get(i).key(), counts[i]);
            if (assign[i] >= 0) out.put(beds.get(i).key(), cols[assign[i]]);
        }
        return new Result(out, countsByBed);
    }

    private static void search(int i, int usedMask, double cur, int[] path, double[][] score, boolean[] ok,
                               double min, int[] best, double[] bestVal) {
        int n = path.length;
        if (i == n) {
            if (cur > bestVal[0]) {
                bestVal[0] = cur;
                System.arraycopy(path, 0, best, 0, n);
            }
            return;
        }
        path[i] = -1; // leave this base unassigned
        search(i + 1, usedMask, cur, path, score, ok, min, best, bestVal);
        for (int c = 0; c < score[i].length; c++) {
            if (!ok[c] || (usedMask & (1 << c)) != 0 || score[i][c] < min) continue;
            path[i] = c;
            search(i + 1, usedMask | (1 << c), cur + score[i][c], path, score, ok, min, best, bestVal);
        }
        path[i] = -1;
    }

    private static int nearestBed(List<BedInfo> beds, int x, int z) {
        int best = 0;
        long bestD = Long.MAX_VALUE;
        for (int i = 0; i < beds.size(); i++) {
            long dx = x - beds.get(i).x();
            long dz = z - beds.get(i).z();
            long d = dx * dx + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }
}
