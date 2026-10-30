package dev.etran.cmd

import dev.etran.EasySetupPlugin
import dev.etran.Msg
import dev.etran.place.NextGen
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.util.Locale

/** /nextgen [diamond|emerald] [degrees] */
class NextGenCommand(private val plugin: EasySetupPlugin) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Players only.")
            return true
        }
        var type: String? = null
        var degrees = plugin.settings().defaultRotation()
        for (a in args) {
            val l = a.lowercase(Locale.ROOT)
            if (l == "diamond" || l == "emerald") {
                type = l
                continue
            }
            degrees = a.toDoubleOrNull() ?: run {
                Msg.err(sender, "Usage: /$label [diamond|emerald] [degrees]")
                return true
            }
        }
        NextGen.run(plugin, sender, type, degrees)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> listOf("diamond", "emerald", "90", "180", "270").filter { it.startsWith(args[0].lowercase(Locale.ROOT)) }
            2 -> listOf("90", "180", "270").filter { it.startsWith(args[1]) }
            else -> emptyList()
        }
    }
}
