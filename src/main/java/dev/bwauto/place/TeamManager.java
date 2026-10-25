package dev.etran.place;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.bw.ActionQueue;
import dev.etran.model.GameModeType;
import dev.etran.model.SetupSession;
import dev.etran.model.TeamColor;
import dev.etran.model.TeamState;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

/** Team creation, removal and gamemode resizing. */
public final class TeamManager {
    private TeamManager() {
    }

    /**
     * Guard used before anything that needs a team to exist. The first failure prints the full hint
     * (use the red concrete on the left of the menu); later failures only play a sound.
     */
    public static boolean requireTeam(Player p, SetupSession s, TeamColor team) {
        if (team != null && s.hasTeam(team)) return true;
        if (!s.warnedMissingTeam) {
            s.warnedMissingTeam = true;
            Msg.err(p, "Team " + (team == null ? "" : team.display + " ") + "does not exist yet. Open /bwsetup and click the "
                    + ChatColor.DARK_RED + "red concrete" + ChatColor.RED
                    + " on the left to auto-create all required teams.");
        } else {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        return false;
    }

    /** Create every required team that does not exist yet and set the max players per team. */
    public static void autoCreate(BwAutoPlugin plugin, Player p, SetupSession s, Runnable after) {
        if (s.gamemode == null) {
            Msg.err(p, "Pick a gamemode first (the nether star in /bwsetup).");
            return;
        }
        if (s.busy) {
            Msg.err(p, "Another placement is still running - wait a moment.");
            return;
        }
        ActionQueue q = new ActionQueue(plugin, p);
        int created = 0;
        for (TeamColor c : s.requiredTeams()) {
            if (s.hasTeam(c)) continue;
            q.command(plugin.bw().build("create-team", "team", c.bwName(), "color", c.bwName()));
            q.then(() -> s.team(c).created = true);
            created++;
        }
        q.command(plugin.bw().build("set-max-in-team", "size", String.valueOf(s.gamemode.size)));
        final int n = created;
        s.busy = true;
        q.done(() -> {
            s.busy = false;
            s.warnedMissingTeam = false;
            if (s.selected == null && !s.requiredTeams().isEmpty()) s.selected = s.requiredTeams().get(0);
            Msg.ok(p, n == 0 ? "All required teams already exist." : "Created " + n + " team(s).");
            if (after != null) after.run();
        });
        q.start();
    }

    /**
     * Switch gamemode. Fewer teams than currently exist: every existing team is deleted and the required
     * ones are recreated. More teams: the missing ones are created. Same count: only the team size changes.
     * With no teams yet, only the mode is recorded - use the red concrete to create the teams.
     */
    public static void applyGamemode(BwAutoPlugin plugin, Player p, SetupSession s, GameModeType nm,
                                     Runnable after) {
        if (s.busy) {
            Msg.err(p, "Another placement is still running - wait a moment.");
            return;
        }
        List<TeamColor> existing = s.createdTeams();
        s.gamemode = nm;
        ActionQueue q = new ActionQueue(plugin, p);
        boolean fewer = !existing.isEmpty() && nm.teams < existing.size();
        boolean more = !existing.isEmpty() && nm.teams > existing.size();

        if (fewer) {
            for (TeamColor c : existing) {
                q.command(plugin.bw().build("remove-team", "team", c.bwName()));
                q.then(() -> wipe(s, c));
            }
        }
        if (fewer || more) {
            for (TeamColor c : s.requiredTeams()) {
                if (!fewer && s.hasTeam(c)) continue;
                q.command(plugin.bw().build("create-team", "team", c.bwName(), "color", c.bwName()));
                q.then(() -> s.team(c).created = true);
            }
        }
        q.command(plugin.bw().build("set-max-in-team", "size", String.valueOf(nm.size)));
        s.busy = true;
        q.done(() -> {
            s.busy = false;
            s.warnedMissingTeam = false;
            if (s.selected == null || (!s.requiredTeams().contains(s.selected))) {
                s.selected = s.requiredTeams().isEmpty() ? null : s.requiredTeams().get(0);
            }
            Msg.ok(p, "Gamemode set to " + nm.label + (fewer ? " (teams were deleted and recreated)." : "."));
            if (after != null) after.run();
        });
        q.start();
    }

    /** Forget everything recorded for a team (it was removed in Bedwars1058). */
    public static void wipe(SetupSession s, TeamColor c) {
        TeamState ts = s.teams.remove(c);
        if (ts != null && ts.bed != null) {
            String key = ts.bed.key();
            s.assignedBeds.removeIf(b -> b.key().equals(key));
        }
    }
}
