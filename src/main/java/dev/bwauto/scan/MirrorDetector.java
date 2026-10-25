package dev.etran.scan;

import dev.etran.Settings;
import dev.etran.model.BedInfo;
import dev.etran.model.Frame;
import dev.etran.model.Offset;
import dev.etran.model.SetupSession;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Decides whether a base is a rotated copy or a mirrored copy of the template base.
 *
 * <p>Blocks around the template bed are sampled in the bed's local frame and compared with the target
 * base under both hypotheses (same local coordinates, and right-axis negated). Block colour is ignored
 * (red wool matches blue wool) so team colours do not matter. The hypothesis with the higher match rate
 * wins; the mirrored one must win by {@code mirror.margin} so symmetric bases default to "rotated".
 */
public final class MirrorDetector {
    private MirrorDetector() {
    }

    public static boolean isMirrored(World w, BedInfo template, BedInfo target, Settings st, SetupSession s) {
        String mode = st.mirrorMode();
        if (mode.equals("off")) return false;
        if (mode.equals("force")) return true;
        if (template.key().equals(target.key())) return false;

        String cacheKey = template.key() + ">" + target.key();
        Boolean cached = s.mirrorCache.get(cacheKey);
        if (cached != null) return cached;

        Frame ft = new Frame(template.x() + 0.5, template.y(), template.z() + 0.5, template.facingYaw(), false);
        Frame fr = new Frame(target.x() + 0.5, target.y(), target.z() + 0.5, target.facingYaw(), false);
        Frame fm = new Frame(target.x() + 0.5, target.y(), target.z() + 0.5, target.facingYaw(), true);

        int r = st.mirrorRadius();
        int total = 0;
        int rotHits = 0;
        int mirHits = 0;
        for (int right = -r; right <= r; right++) {
            for (int fwd = -r; fwd <= r; fwd++) {
                for (int up = -4; up <= 8; up++) {
                    Offset o = new Offset(right, up, fwd, 0);
                    Material tm = blockAt(w, ft, o);
                    if (tm.isAir()) continue;
                    total++;
                    String fam = Mats.family(tm);
                    if (Mats.family(blockAt(w, fr, o)).equals(fam)) rotHits++;
                    if (Mats.family(blockAt(w, fm, o)).equals(fam)) mirHits++;
                }
            }
        }
        boolean mirrored = false;
        if (total > 0) {
            double rot = rotHits / (double) total;
            double mir = mirHits / (double) total;
            mirrored = mir > rot + st.mirrorMargin();
        }
        s.mirrorCache.put(cacheKey, mirrored);
        return mirrored;
    }

    private static Material blockAt(World w, Frame f, Offset o) {
        double[] p = f.toWorld(o);
        return w.getBlockAt((int) Math.floor(p[0]), (int) Math.floor(p[1] + 1e-6), (int) Math.floor(p[2])).getType();
    }
}
