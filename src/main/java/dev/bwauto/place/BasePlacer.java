package dev.etran.place;

import dev.etran.BwAutoPlugin;
import dev.etran.bw.ActionQueue;
import dev.etran.bw.ElementPlacer;
import dev.etran.model.BedInfo;
import dev.etran.model.Element;
import dev.etran.model.Frame;
import dev.etran.model.Offset;
import dev.etran.model.SetupSession;
import dev.etran.model.Template;
import dev.etran.model.TeamColor;
import dev.etran.model.TeamState;
import dev.etran.scan.WallResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Applies the template to another base: the saved offsets are re-anchored on that base's bed (rotated by
 * the difference in bed facing, mirrored if the base was detected as a mirror image), shop/generator
 * positions are pulled out of walls by raycasting, existing generators are removed first, and the Bedwars
 * commands are queued. The bed is processed last, like the Skript.
 */
public final class BasePlacer {
    private static final List<Element> ORDER = List.of(
            Element.SPAWN, Element.SHOP, Element.UPGRADE, Element.GEN_IRON, Element.GEN_GOLD, Element.KILL_DROPS);
    private static final EnumSet<Element> RELOCATE = EnumSet.of(
            Element.SHOP, Element.UPGRADE, Element.GEN_IRON, Element.GEN_GOLD, Element.KILL_DROPS);

    private BasePlacer() {
    }

    /** Resolve a template element for a target bed without queueing anything (used for previews/tests). */
    public static Location locate(Template t, Element e, BedInfo bed, boolean mirror, boolean snap, World w) {
        Offset o = t.offset(e);
        if (o == null) return null;
        Frame f = new Frame(bed.x() + 0.5, bed.y(), bed.z() + 0.5, bed.facingYaw(), mirror);
        return at(w, f, o, snap);
    }

    /**
     * Append the steps that place {@code team} at {@code bed} to the queue.
     *
     * @return the labels of template elements that were skipped because they were never captured
     */
    public static List<String> queue(BwAutoPlugin plugin, SetupSession s, ActionQueue q, TeamColor team,
                                     BedInfo bed, boolean mirror) {
        World w = Bukkit.getWorld(bed.world());
        List<String> skipped = new ArrayList<>();
        if (w == null) return skipped;
        Template t = s.template;
        boolean snap = plugin.settings().snap();
        Frame frame = new Frame(bed.x() + 0.5, bed.y(), bed.z() + 0.5, bed.facingYaw(), mirror);
        TeamState ts = s.team(team);

        Offset so = t.offset(Element.SPAWN);
        Location spawn = so == null ? null : at(w, frame, so, snap);
        String removeCmd = plugin.bw().build("remove-generator");

        for (Element e : ORDER) {
            Offset o = t.offset(e);
            if (o == null) {
                skipped.add(e.label);
                continue;
            }
            Location loc = at(w, frame, o, snap);
            if (spawn != null && RELOCATE.contains(e)) loc = WallResolver.resolve(spawn, loc);
            final Location target = loc;
            final Element el = e;

            // A generator of this type already placed at this base: delete it before placing the new one.
            String type = e.genType();
            if (type != null && removeCmd != null) {
                TeamState.GenRecord existing = ElementPlacer.findGen(ts, type, target);
                if (existing != null) {
                    q.teleport(existing.loc());
                    q.command(removeCmd);
                    q.then(() -> ts.gens.remove(existing));
                }
            }

            q.teleport(target);
            q.command(ElementPlacer.commandFor(plugin.bw(), team, e));
            q.then(() -> {
                ts.placed.add(el);
                if (el.isGenerator()) ts.gens.add(new TeamState.GenRecord(el.genType(), target.clone()));
            });
        }

        // Bed last: stand on it so the setup command picks up the right block.
        q.teleport(bed.footCenter(w));
        q.command(ElementPlacer.commandFor(plugin.bw(), team, Element.BED));
        q.then(() -> {
            ts.placed.add(Element.BED);
            ts.bed = bed;
            s.addAssigned(bed);
        });
        return skipped;
    }

    private static Location at(World w, Frame f, Offset o, boolean snap) {
        double[] v = f.toWorld(o);
        double x = v[0];
        double z = v[2];
        if (snap) {
            x = Math.round(x * 2.0) / 2.0;
            z = Math.round(z * 2.0) / 2.0;
        }
        return new Location(w, x, v[1], z, (float) v[3], 0f);
    }
}
