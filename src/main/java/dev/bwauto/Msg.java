package dev.etran;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Msg {
    public static final String PREFIX = ChatColor.GOLD + "[BWAuto] " + ChatColor.RESET;

    private Msg() {
    }

    public static void ok(CommandSender to, String m) {
        to.sendMessage(PREFIX + ChatColor.GREEN + m);
    }

    public static void err(CommandSender to, String m) {
        to.sendMessage(PREFIX + ChatColor.RED + m);
    }

    public static void info(CommandSender to, String m) {
        to.sendMessage(PREFIX + ChatColor.YELLOW + m);
    }

    public static void plain(CommandSender to, String m) {
        to.sendMessage(m);
    }
}
