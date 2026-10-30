package dev.etran

import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

object Msg {
    val PREFIX: String = ChatColor.GOLD.toString() + "[EasySetup] " + ChatColor.RESET

    @JvmStatic
    fun ok(to: CommandSender, m: String) = to.sendMessage(PREFIX + ChatColor.GREEN + m)

    @JvmStatic
    fun err(to: CommandSender, m: String) = to.sendMessage(PREFIX + ChatColor.RED + m)

    @JvmStatic
    fun info(to: CommandSender, m: String) = to.sendMessage(PREFIX + ChatColor.YELLOW + m)

    @JvmStatic
    fun plain(to: CommandSender, m: String) = to.sendMessage(m)
}
