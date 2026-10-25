package dev.etran.cmd;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.place.AutoRunner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /bwauto [run|next|confirm|deny] */
public final class AutoCommand implements TabExecutor {
    private final BwAutoPlugin plugin;

    public AutoCommand(BwAutoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        String sub = args.length == 0 ? "run" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "run" -> AutoRunner.request(plugin, p, false);
            case "next" -> AutoRunner.request(plugin, p, true);
            case "confirm" -> AutoRunner.confirm(plugin, p);
            case "deny" -> AutoRunner.deny(plugin, p);
            default -> Msg.err(p, "Usage: /" + label + " [run|next|confirm|deny]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("run", "next", "confirm", "deny")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        }
        return out;
    }
}
