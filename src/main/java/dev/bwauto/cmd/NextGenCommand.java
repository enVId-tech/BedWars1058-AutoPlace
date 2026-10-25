package dev.etran.cmd;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.place.NextGen;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /nextgen [diamond|emerald] [degrees]  (alias /bwrotate)
 *
 * <p>Examples: {@code /nextgen diamond} detects and places the next diamond generator, falling back to a
 * 90 degree rotation; {@code /nextgen 45} rotates by 45 degrees; {@code /nextgen emerald 180}.
 */
public final class NextGenCommand implements TabExecutor {
    private final BwAutoPlugin plugin;

    public NextGenCommand(BwAutoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        String type = null;
        double degrees = plugin.settings().defaultRotation();
        for (String a : args) {
            String l = a.toLowerCase(Locale.ROOT);
            if (l.equals("diamond") || l.equals("emerald")) {
                type = l;
                continue;
            }
            try {
                degrees = Double.parseDouble(a);
            } catch (NumberFormatException ex) {
                Msg.err(p, "Usage: /" + label + " [diamond|emerald] [degrees]");
                return true;
            }
        }
        NextGen.run(plugin, p, type, degrees);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("diamond", "emerald", "90", "180", "270")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2) {
            for (String s : List.of("90", "180", "270")) {
                if (s.startsWith(args[1])) out.add(s);
            }
        }
        return out;
    }
}
