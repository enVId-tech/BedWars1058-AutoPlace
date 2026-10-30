package dev.etran.cmd

import dev.etran.EasySetupPlugin
import dev.etran.Msg
import dev.etran.place.AutoRunner
import dev.etran.scan.GeneratorScanner
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.util.Locale

/** /bwauto [run|next|confirm|deny|gens] */
class AutoCommand(private val plugin: EasySetupPlugin) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Players only.")
            return true
        }
        when (if (args.isEmpty()) "run" else args[0].lowercase(Locale.ROOT)) {
            "run" -> AutoRunner.request(plugin, sender, false)
            "next" -> AutoRunner.request(plugin, sender, true)
            "confirm" -> AutoRunner.confirm(plugin, sender)
            "deny" -> AutoRunner.deny(plugin, sender)
            "gens" -> gens(sender)
            else -> Msg.err(sender, "Usage: /$label [run|next|confirm|deny|gens]")
        }
        return true
    }

    /** Lists every diamond/emerald candidate with its probability and the clues behind it. */
    private fun gens(p: Player) {
        val s = plugin.sessions().get(p)
        val res = GeneratorScanner.scan(p.world, p.location, plugin.settings())
        val lines = GeneratorScanner.describe(res, plugin.settings(), p.world, s)
        if (lines.isEmpty()) {
            Msg.info(
                p,
                "No diamond or emerald blocks found within ${plugin.settings().generatorScanRadius()} blocks of the map " +
                    "centre (and ${plugin.settings().generatorYRange()} blocks of your height)."
            )
            return
        }
        Msg.info(
            p,
            "Generator candidates (accepted at ${plugin.settings().generatorMinProbability()}+, map " +
                (if (res.symmetric) "looks symmetric" else "does not look symmetric") + "):"
        )
        for (l in lines) Msg.plain(p, ChatColor.GRAY.toString() + l)
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (args.size != 1) return emptyList()
        return listOf("run", "next", "confirm", "deny", "gens").filter { it.startsWith(args[0].lowercase(Locale.ROOT)) }
    }
}
