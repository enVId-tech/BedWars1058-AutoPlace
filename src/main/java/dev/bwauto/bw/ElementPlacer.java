package dev.etran.bw;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.model.BedInfo;
import dev.etran.model.Element;
import dev.etran.model.SetupMode;
import dev.etran.model.SetupSession;
import dev.etran.model.Template;
import dev.etran.model.TeamColor;
import dev.etran.model.TeamState;
import dev.etran.place.TeamManager;
import dev.etran.scan.BedScanner;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Places a single element at the player's position by running the matching Bedwars command, then records
 * it in the session. In semi/auto mode the position is also captured into the base template, which is how
 * the position delta and rotation delta relative to the bed get saved.
 */
public final class ElementPlacer {
    private ElementPlacer() {
    }

    public static String commandFor(BwCommands bw, TeamColor team, Element e) {
        String tn = team == null ? "" : team.bwName();
        return switch (e) {
            case SPAWN -> bw.build("set-spawn", "team", tn);
            case BED -> bw.build("set-bed", "team", tn);
            case SHOP -> bw.build("set-shop", "team", tn);
            case UPGRADE -> bw.build("set-upgrade", "team", tn);
            case KILL_DROPS -> bw.build("set-kill-drops", "team", tn);
            case GEN_IRON -> bw.build("add-generator", "type", "iron", "team", tn);
            case GEN_GOLD -> bw.build("add-generator", "type", "gold", "team", tn);
            case WAITING_SPAWN -> bw.build("set-waiting-spawn");
            case SPECTATOR_SPAWN -> bw.build("set-spectator-spawn");
            case LOBBY_POS1 -> bw.build("waiting-pos1");
            case LOBBY_POS2 -> bw.build("waiting-pos2");
            case GEN_DIAMOND -> bw.build("add-map-generator", "type", "diamond");
            case GEN_EMERALD -> bw.build("add-map-generator", "type", "emerald");
        };
    }

    /** Place one element at the player's current position. */
    public static void placeHere(BwAutoPlugin plugin, Player p, SetupSession s, TeamColor team, Element e,
                                 Runnable after) {
        placeMany(plugin, p, s, team, List.of(e), after);
    }

    /** Place several elements at the player's current position, in order (e.g. the forge combo). */
    public static void placeMany(BwAutoPlugin plugin, Player p, SetupSession s, TeamColor team,
                                 List<Element> elems, Runnable after) {
        if (s.busy) {
            Msg.err(p, "Another placement is still running - wait a moment.");
            return;
        }
        boolean needsTeam = false;
        for (Element e : elems) needsTeam |= e.perTeam;
        if (needsTeam && !TeamManager.requireTeam(p, s, team)) return;

        Location loc = p.getLocation().clone();
        ActionQueue q = new ActionQueue(plugin, p);
        for (Element e : elems) {
            String cmd = commandFor(plugin.bw(), team, e);
            if (cmd == null) {
                Msg.err(p, "No command is configured for " + e.label + " (bw-commands in config.yml).");
                continue;
            }
            // Re-placing a generator: remove the one we placed here before, so output is not doubled.
            String type = e.genType();
            if (type != null) {
                String rm = plugin.bw().build("remove-generator");
                if (e.perTeam && team != null) {
                    TeamState ts = s.team(team);
                    TeamState.GenRecord ex = findGen(ts, type, loc);
                    if (ex != null && rm != null) {
                        q.command(rm);
                        q.then(() -> ts.gens.remove(ex));
                    }
                } else {
                    Location ex = findMapGen(s, e, loc);
                    if (ex != null && rm != null) {
                        q.command(rm);
                        q.then(() -> s.mapElements.get(e).remove(ex));
                    }
                }
            }
            q.command(cmd);
            q.then(() -> record(plugin, p, s, team, e, loc));
        }
        s.busy = true;
        q.done(() -> {
            s.busy = false;
            if (after != null) after.run();
        });
        q.start();
    }

    /** Mark an element as placed (and capture it into the template). The Bedwars command already ran. */
    public static void record(BwAutoPlugin plugin, Player p, SetupSession s, TeamColor team, Element e,
                              Location loc) {
        if (e.perTeam) {
            if (team == null) return;
            TeamState ts = s.team(team);
            ts.placed.add(e);
            if (e.isGenerator()) ts.gens.add(new TeamState.GenRecord(e.genType(), loc.clone()));
            if (s.mode != SetupMode.MANUAL) capture(plugin, s, team, e, loc);
        } else if (e.isGenerator()) {
            s.addMapGen(e, loc);
        } else {
            s.setMap(e, loc);
        }
    }

    private static void capture(BwAutoPlugin plugin, SetupSession s, TeamColor team, Element e, Location loc) {
        Template t = s.template;
        if (t.team() == null) t.setTeam(team);
        if (t.team() != team) return; // only the template team defines the deltas

        if (e == Element.BED) {
            BedInfo b = BedScanner.nearest(loc.getWorld(), loc, plugin.settings().templateBedRadius());
            if (b != null) {
                t.setBed(b);
                s.team(team).bed = b;
                s.addAssigned(b);
            }
            return;
        }
        t.capture(e, loc, plugin.settings().snap());
        if (t.bed() == null) {
            BedInfo b = BedScanner.nearest(loc.getWorld(), loc, plugin.settings().templateBedRadius());
            if (b != null) {
                t.setBed(b);
                s.addAssigned(b);
            }
        }
    }

    public static TeamState.GenRecord findGen(TeamState ts, String type, Location near) {
        for (TeamState.GenRecord r : ts.gens) {
            if (r.type().equals(type) && sameWorld(r.loc(), near) && r.loc().distance(near) < 1.5) return r;
        }
        return null;
    }

    private static Location findMapGen(SetupSession s, Element e, Location near) {
        List<Location> l = s.mapElements.get(e);
        if (l == null) return null;
        for (Location r : l) {
            if (sameWorld(r, near) && r.distance(near) < 1.5) return r;
        }
        return null;
    }

    private static boolean sameWorld(Location a, Location b) {
        return a.getWorld() != null && a.getWorld().equals(b.getWorld());
    }
}
