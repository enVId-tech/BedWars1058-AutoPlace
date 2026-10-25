package dev.etran.place;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.Settings;
import dev.etran.bw.ActionQueue;
import dev.etran.bw.ElementPlacer;
import dev.etran.model.BedInfo;
import dev.etran.model.Element;
import dev.etran.model.SetupMode;
import dev.etran.model.SetupSession;
import dev.etran.model.Template;
import dev.etran.model.TeamColor;
import dev.etran.scan.BedScanner;
import dev.etran.scan.GeneratorScanner;
import dev.etran.scan.LobbyDetector;
import dev.etran.scan.MirrorDetector;
import dev.etran.scan.WoolDetector;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatic placement. {@link #request} is the entry point used by /bwauto and the menus; it runs the
 * "missing elements" check and either starts, or asks for /bwauto confirm|deny.
 *
 * <ul>
 *   <li>Fully automatic: every remaining base, the lobby structure and all detectable generators.</li>
 *   <li>Semi-automatic ({@code singleBase}): one base per click, then the player is moved onto the next
 *       unassigned bed - the Skript's /createteam flow.</li>
 * </ul>
 */
public final class AutoRunner {
    private AutoRunner() {
    }

    public static void request(BwAutoPlugin plugin, Player p, boolean singleBase) {
        SetupSession s = plugin.sessions().get(p);
        if (s.mode == SetupMode.MANUAL) {
            Msg.err(p, "Automatic placement is disabled in Manual mode. Switch mode in /bwsetup.");
            return;
        }
        if (s.busy) {
            Msg.err(p, "Another placement is still running - wait a moment.");
            return;
        }
        if (s.gamemode == null) {
            Msg.err(p, "Pick a gamemode first (the nether star in /bwsetup).");
            return;
        }
        List<Element> missing = Validator.missingTemplate(s);
        if (!missing.isEmpty() && !s.confirmedIncomplete) {
            s.pendingAuto = true;
            s.pendingSingle = singleBase;
            p.sendMessage(ChatColor.RED + plugin.settings().incompleteAutoMessage(Element.join(missing)));
            return;
        }
        s.confirmedIncomplete = false;
        s.pendingAuto = false;
        if (singleBase) {
            nextBase(plugin, p, s);
        } else {
            runAll(plugin, p, s);
        }
    }

    public static void confirm(BwAutoPlugin plugin, Player p) {
        SetupSession s = plugin.sessions().get(p);
        if (!s.pendingAuto) {
            Msg.info(p, "Nothing is waiting for confirmation.");
            return;
        }
        s.confirmedIncomplete = true;
        request(plugin, p, s.pendingSingle);
    }

    public static void deny(BwAutoPlugin plugin, Player p) {
        SetupSession s = plugin.sessions().get(p);
        if (!s.pendingAuto) {
            Msg.info(p, "Nothing is waiting for confirmation.");
            return;
        }
        s.pendingAuto = false;
        s.confirmedIncomplete = false;
        Msg.info(p, "Automatic placement cancelled. Place the missing elements, then run /bwauto again.");
    }

    // ------------------------------------------------------------------------------------------
    // shared preparation
    // ------------------------------------------------------------------------------------------

    private static boolean prepare(BwAutoPlugin plugin, Player p, SetupSession s) {
        for (TeamColor c : s.requiredTeams()) {
            if (!TeamManager.requireTeam(p, s, c)) return false;
        }
        Template t = s.template;
        if (t.team() == null) {
            t.setTeam(s.selected != null ? s.selected : s.requiredTeams().get(0));
        }
        if (t.bed() == null) {
            BedInfo b = BedScanner.nearest(p.getWorld(), p.getLocation(), plugin.settings().templateBedRadius());
            if (b != null) {
                t.setBed(b);
                s.addAssigned(b);
            }
        }
        if (t.bed() == null) {
            Msg.err(p, "No template bed found. Stand near the base you set up, or place its bed element first.");
            return false;
        }
        if (t.offset(Element.SPAWN) == null) {
            Msg.err(p, "The template base has no spawn captured, so there is nothing to copy. Place the spawn first.");
            return false;
        }
        return true;
    }

    private static Location centre(Settings st, World w, Location ref) {
        return new Location(w, st.centerX(), ref.getY(), st.centerZ());
    }

    // ------------------------------------------------------------------------------------------
    // fully automatic
    // ------------------------------------------------------------------------------------------

    private static void runAll(BwAutoPlugin plugin, Player p, SetupSession s) {
        if (!prepare(plugin, p, s)) return;
        Settings st = plugin.settings();
        World w = p.getWorld();
        Template t = s.template;
        Location start = p.getLocation().clone();

        List<BedInfo> unassigned = BedScanner.unassigned(s, st, w, centre(st, w, start));
        List<BedInfo> all = new ArrayList<>();
        all.add(t.bed());
        all.addAll(unassigned);
        WoolDetector.Result wool = WoolDetector.detect(w, all, st, s.requiredTeams());

        TeamColor seen = wool.byBed().get(t.bed().key());
        if (seen != null && seen != t.team()) {
            Msg.info(p, "The wool at the template base looks " + seen.display + " but you set it up as "
                    + t.team().display + ".");
        }

        List<TeamColor> remaining = remainingTeams(s);
        Map<BedInfo, TeamColor> plan = assign(unassigned, remaining, wool);
        if (unassigned.size() > remaining.size()) {
            Msg.info(p, "Found " + unassigned.size() + " unassigned beds for " + remaining.size()
                    + " remaining team(s); extra beds are ignored.");
        } else if (unassigned.size() < remaining.size()) {
            Msg.info(p, "Only " + unassigned.size() + " unassigned bed(s) found for " + remaining.size()
                    + " remaining team(s).");
        }

        p.closeInventory();
        ActionQueue q = new ActionQueue(plugin, p);
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<BedInfo, TeamColor> en : plan.entrySet()) {
            boolean mirror = MirrorDetector.isMirrored(w, t.bed(), en.getKey(), st, s);
            List<String> skipped = BasePlacer.queue(plugin, s, q, en.getValue(), en.getKey(), mirror);
            summary.append(en.getValue().display).append(mirror ? " (mirrored) " : " ");
            if (!skipped.isEmpty()) {
                Msg.info(p, en.getValue().display + ": template has no " + String.join(", ", skipped) + ".");
            }
        }

        queueLobby(plugin, p, s, q, w);
        int[] gens = queueGenerators(plugin, p, s, q, w, start);

        s.busy = true;
        q.teleport(start);
        q.done(() -> {
            s.busy = false;
            Msg.ok(p, "Automatic placement finished. Bases: "
                    + (summary.length() == 0 ? "none" : summary.toString().trim())
                    + ". Generators queued: " + gens[0] + " diamond, " + gens[1] + " emerald.");
            Msg.info(p, "Open /bwsetup and use the green concrete to validate and save.");
        });
        q.start();
    }

    // ------------------------------------------------------------------------------------------
    // semi-automatic: one base, then walk to the next bed
    // ------------------------------------------------------------------------------------------

    private static void nextBase(BwAutoPlugin plugin, Player p, SetupSession s) {
        if (!prepare(plugin, p, s)) return;
        Settings st = plugin.settings();
        World w = p.getWorld();
        Template t = s.template;
        Location start = p.getLocation().clone();

        List<BedInfo> unassigned = BedScanner.unassigned(s, st, w, centre(st, w, start));
        if (unassigned.isEmpty()) {
            Msg.info(p, "No further unassigned base islands detected on the map.");
            return;
        }
        BedInfo target = unassigned.get(0);
        double bestD = Double.MAX_VALUE;
        for (BedInfo b : unassigned) {
            double d = b.footCenter(w).distanceSquared(start);
            if (d < bestD) {
                bestD = d;
                target = b;
            }
        }

        List<BedInfo> all = new ArrayList<>();
        all.add(t.bed());
        all.addAll(unassigned);
        WoolDetector.Result wool = WoolDetector.detect(w, all, st, s.requiredTeams());
        List<TeamColor> remaining = remainingTeams(s);
        if (remaining.isEmpty()) {
            Msg.info(p, "Every required team already has a bed.");
            return;
        }
        TeamColor color = wool.byBed().get(target.key());
        if (color == null || !remaining.contains(color)) color = remaining.get(0);

        boolean mirror = MirrorDetector.isMirrored(w, t.bed(), target, st, s);
        p.closeInventory();
        ActionQueue q = new ActionQueue(plugin, p);
        List<String> skipped = BasePlacer.queue(plugin, s, q, color, target, mirror);
        if (!skipped.isEmpty()) Msg.info(p, "Template has no " + String.join(", ", skipped) + ".");

        final TeamColor placed = color;
        final BedInfo done = target;
        s.busy = true;
        q.teleport(start);
        q.done(() -> {
            s.busy = false;
            Msg.ok(p, "Placed team " + placed.display + (mirror ? " (mirrored)" : "") + ".");
            List<BedInfo> left = BedScanner.unassigned(s, st, w, centre(st, w, start));
            left.removeIf(b -> b.key().equals(done.key()));
            if (left.isEmpty()) {
                Msg.info(p, "No further unassigned base islands detected on the map.");
                return;
            }
            BedInfo next = left.get(0);
            double nd = Double.MAX_VALUE;
            for (BedInfo b : left) {
                double d = b.footCenter(w).distanceSquared(p.getLocation());
                if (d < nd) {
                    nd = d;
                    next = b;
                }
            }
            p.teleport(standOn(next, st, w));
            Msg.ok(p, "Teleported onto the bed of the next unassigned base island.");
        });
        q.start();
    }

    /** Standing on the bed, facing the map centre, snapped to 90 degrees. */
    private static Location standOn(BedInfo bed, Settings st, World w) {
        Location l = bed.footCenter(w).add(0, 1, 0);
        double dx = st.centerX() - l.getX();
        double dz = st.centerZ() - l.getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        yaw = Math.round(yaw / 90.0) * 90.0;
        l.setYaw((float) yaw);
        l.setPitch(0f);
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------

    /** Required teams that have no bed yet (the template team is excluded - it is already set up). */
    private static List<TeamColor> remainingTeams(SetupSession s) {
        List<TeamColor> out = new ArrayList<>();
        for (TeamColor c : s.requiredTeams()) {
            if (c == s.template.team()) continue;
            if (!s.teamPlaced(c, Element.BED)) out.add(c);
        }
        return out;
    }

    /** Prefer the colour detected from each base's wool; hand out leftover teams in order. */
    private static Map<BedInfo, TeamColor> assign(List<BedInfo> beds, List<TeamColor> teams,
                                                  WoolDetector.Result wool) {
        Map<BedInfo, TeamColor> plan = new LinkedHashMap<>();
        List<TeamColor> pool = new ArrayList<>(teams);
        for (BedInfo b : beds) {
            TeamColor c = wool.byBed().get(b.key());
            if (c != null && pool.remove(c)) plan.put(b, c);
        }
        for (BedInfo b : beds) {
            if (plan.containsKey(b) || pool.isEmpty()) continue;
            plan.put(b, pool.remove(0));
        }
        return plan;
    }

    private static void queueLobby(BwAutoPlugin plugin, Player p, SetupSession s, ActionQueue q, World w) {
        boolean wanted = s.mode == SetupMode.AUTO || s.autoLobby;
        boolean haveAll = s.mapPlaced(Element.WAITING_SPAWN) && s.mapPlaced(Element.SPECTATOR_SPAWN)
                && s.mapPlaced(Element.LOBBY_POS1) && s.mapPlaced(Element.LOBBY_POS2);
        if (!wanted || haveAll) return;
        queueLobbyDetection(plugin, p, s, q, w);
    }

    /** Detect the lobby structure and queue its markers. Also used directly by the lobby menu. */
    public static boolean queueLobbyDetection(BwAutoPlugin plugin, Player p, SetupSession s, ActionQueue q,
                                              World w) {
        LobbyDetector.Result r = LobbyDetector.detect(w, plugin.settings());
        if (r == null) {
            Msg.err(p, "Could not detect a lobby structure near the map centre. Place the lobby markers by hand "
                    + "(Lobby & Boundary menu).");
            return false;
        }
        Location centre = r.center();
        Location c1 = r.corner1();
        Location c2 = r.corner2();
        var bw = plugin.bw();
        q.teleport(centre);
        q.command(ElementPlacer.commandFor(bw, null, Element.WAITING_SPAWN));
        q.then(() -> s.setMap(Element.WAITING_SPAWN, centre));
        q.command(ElementPlacer.commandFor(bw, null, Element.SPECTATOR_SPAWN));
        q.then(() -> s.setMap(Element.SPECTATOR_SPAWN, centre));
        q.teleport(c1);
        q.command(ElementPlacer.commandFor(bw, null, Element.LOBBY_POS1));
        q.then(() -> s.setMap(Element.LOBBY_POS1, c1));
        q.teleport(c2);
        q.command(ElementPlacer.commandFor(bw, null, Element.LOBBY_POS2));
        q.then(() -> s.setMap(Element.LOBBY_POS2, c2));
        Msg.info(p, "Lobby structure detected (" + r.blocks() + " blocks): box (" + r.minX() + ", " + r.minY()
                + ", " + r.minZ() + ") to (" + r.maxX() + ", " + r.maxY() + ", " + r.maxZ()
                + "), spawns at its centre.");
        return true;
    }

    /** Queue placement of every detectable, still-unplaced diamond and emerald generator. */
    public static int[] queueGenerators(BwAutoPlugin plugin, Player p, SetupSession s, ActionQueue q, World w,
                                        Location around) {
        int[] counts = new int[2];
        String[] types = {"diamond", "emerald"};
        Element[] els = {Element.GEN_DIAMOND, Element.GEN_EMERALD};
        for (int i = 0; i < 2; i++) {
            final String type = types[i];
            final Element el = els[i];
            List<Location> found = GeneratorScanner.findUnplaced(w, around, type, plugin.settings(), s);
            for (Location spot : found) {
                q.teleport(spot);
                q.command(ElementPlacer.commandFor(plugin.bw(), null, el));
                q.then(() -> s.addMapGen(el, spot));
                counts[i]++;
            }
            if (found.isEmpty()) {
                Msg.info(p, "No unplaced " + type + " generators detected.");
            }
        }
        return counts;
    }

    /** Used by menus to know how many team slots are left. */
    public static int remainingCount(SetupSession s) {
        return remainingTeams(s).size();
    }
}
