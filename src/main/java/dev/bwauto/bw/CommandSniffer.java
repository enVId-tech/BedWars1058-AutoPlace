package dev.etran.bw;

import dev.etran.BwAutoPlugin;
import dev.etran.model.Element;
import dev.etran.model.SetupSession;
import dev.etran.model.TeamColor;
import dev.etran.model.TeamState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;

/**
 * Port of the Skript's "on command" sniffer. When a player with an open setup session types a Bedwars
 * setup command by hand, the element is recorded (and captured into the template) exactly as if it had
 * been placed from the menu. Commands this plugin runs itself use performCommand, which does not fire
 * this event, so nothing is recorded twice.
 */
public final class CommandSniffer implements Listener {
    private final BwAutoPlugin plugin;

    public CommandSniffer(BwAutoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent ev) {
        Player p = ev.getPlayer();
        SetupSession s = plugin.sessions().peek(p);
        if (s == null) return;

        String msg = ev.getMessage().trim();
        if (msg.startsWith("/")) msg = msg.substring(1);
        String[] a = msg.split("\\s+");
        if (a.length < 2 || !plugin.settings().commandRoots().contains(a[0].toLowerCase(Locale.ROOT))) return;

        String sub = a[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "createteam" -> {
                TeamColor c = a.length > 2 ? TeamColor.fromName(a[2]) : null;
                if (c != null) s.team(c).created = true;
            }
            case "removeteam" -> {
                TeamColor c = a.length > 2 ? TeamColor.fromName(a[2]) : null;
                if (c != null) {
                    TeamState ts = s.teams.remove(c);
                    if (ts != null && ts.bed != null) {
                        s.assignedBeds.removeIf(b -> b.key().equals(ts.bed.key()));
                    }
                }
            }
            case "setspawn" -> teamElement(p, s, a, 2, Element.SPAWN);
            case "setbed" -> teamElement(p, s, a, 2, Element.BED);
            case "setshop" -> teamElement(p, s, a, 2, Element.SHOP);
            case "setupgrade" -> teamElement(p, s, a, 2, Element.UPGRADE);
            case "setkilldrops" -> teamElement(p, s, a, 2, Element.KILL_DROPS);
            case "addgenerator" -> {
                if (a.length < 3) return;
                String type = a[2].toLowerCase(Locale.ROOT);
                switch (type) {
                    case "iron" -> teamElement(p, s, a, 3, Element.GEN_IRON);
                    case "gold" -> teamElement(p, s, a, 3, Element.GEN_GOLD);
                    case "diamond" -> ElementPlacer.record(plugin, p, s, null, Element.GEN_DIAMOND, p.getLocation());
                    case "emerald" -> ElementPlacer.record(plugin, p, s, null, Element.GEN_EMERALD, p.getLocation());
                    default -> {
                    }
                }
            }
            case "setwaitingspawn" ->
                    ElementPlacer.record(plugin, p, s, null, Element.WAITING_SPAWN, p.getLocation());
            case "setspectatorspawn" ->
                    ElementPlacer.record(plugin, p, s, null, Element.SPECTATOR_SPAWN, p.getLocation());
            case "waitingpos1" -> ElementPlacer.record(plugin, p, s, null, Element.LOBBY_POS1, p.getLocation());
            case "waitingpos2" -> ElementPlacer.record(plugin, p, s, null, Element.LOBBY_POS2, p.getLocation());
            default -> {
            }
        }
    }

    private void teamElement(Player p, SetupSession s, String[] a, int teamArg, Element e) {
        TeamColor c = a.length > teamArg ? TeamColor.fromName(a[teamArg]) : s.selected;
        if (c == null) return;
        ElementPlacer.record(plugin, p, s, c, e, p.getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent ev) {
        plugin.sessions().remove(ev.getPlayer().getUniqueId());
    }
}
