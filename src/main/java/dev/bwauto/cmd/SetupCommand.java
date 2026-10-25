package dev.etran.cmd;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.gui.MainMenu;
import dev.etran.model.GameModeType;
import dev.etran.model.SetupMode;
import dev.etran.model.SetupSession;
import dev.etran.place.TeamManager;
import dev.etran.place.Validator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /bwsetup [open | mode <manual|semi|auto> | gamemode <1v1v1v1|...> | status | reset] */
public final class SetupCommand implements TabExecutor {
    private final BwAutoPlugin plugin;

    public SetupCommand(BwAutoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        SetupSession s = plugin.sessions().get(p);
        String sub = args.length == 0 ? "open" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "open" -> new MainMenu(plugin, p).open();
            case "mode" -> {
                SetupMode m = args.length > 1 ? SetupMode.parse(args[1]) : null;
                if (m == null) {
                    Msg.err(p, "Usage: /" + label + " mode <manual|semi|auto>");
                    return true;
                }
                s.mode = m;
                Msg.ok(p, "Setup mode: " + m.label);
            }
            case "gamemode" -> {
                GameModeType g = args.length > 1 ? GameModeType.parse(args[1]) : null;
                if (g == null) {
                    Msg.err(p, "Usage: /" + label + " gamemode <1v1v1v1|1v1|2v2v2v2|2v2|3v3v3v3|3v3|4v4v4v4|4v4>");
                    return true;
                }
                TeamManager.applyGamemode(plugin, p, s, g, null);
            }
            case "status" -> {
                Msg.info(p, "Mode: " + s.mode.label + ", gamemode: " + (s.gamemode == null ? "none" : s.gamemode.label));
                List<String> missing = Validator.missingAll(s, plugin.settings());
                if (missing.isEmpty()) {
                    Msg.ok(p, "Nothing is missing.");
                } else {
                    for (String line : missing) Msg.plain(p, " - " + line);
                }
            }
            case "reset" -> {
                s.reset();
                Msg.ok(p, "Setup state cleared (this does not delete anything inside Bedwars1058).");
            }
            default -> Msg.err(p, "Usage: /" + label + " [open|mode|gamemode|status|reset]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("open", "mode", "gamemode", "status", "reset")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            for (String s : List.of("manual", "semi", "auto")) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("gamemode")) {
            for (GameModeType g : GameModeType.values()) {
                if (g.label.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(g.label);
            }
        }
        return out;
    }
}
