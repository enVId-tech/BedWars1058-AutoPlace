package dev.etran.cmd

import dev.etran.EasySetupPlugin
import dev.etran.Msg
import dev.etran.gui.MainMenu
import dev.etran.model.Facing
import dev.etran.model.GameModeType
import dev.etran.model.SetupMode
import dev.etran.model.SetupSession
import dev.etran.place.FacingPolicy
import dev.etran.place.TeamManager
import dev.etran.place.Validator
import dev.etran.tools.ArenaImporter
import dev.etran.tools.Hotbar
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.util.Locale

/** /bwsetup [open|exit|tools|page|mode|gamemode|facing|snap|pitch|import|status|reset] */
class SetupCommand(private val plugin: EasySetupPlugin) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Players only.")
            return true
        }
        val p: Player = sender
        val s = plugin.sessions().get(p)
        when (if (args.isEmpty()) "open" else args[0].lowercase(Locale.ROOT)) {
            "open" -> {
                Hotbar.enter(plugin, p, s)
                MainMenu(plugin, p).open()
            }
            "exit" -> {
                if (s.toolsActive) {
                    Hotbar.deactivate(plugin, p, s)
                    Msg.ok(p, "Setup tools removed; your hotbar is back.")
                } else {
                    Msg.info(p, "Setup tools are not active.")
                }
            }
            "tools" -> {
                if (s.toolsActive) {
                    Hotbar.deactivate(plugin, p, s)
                    Msg.ok(p, "Setup tools removed; your hotbar is back.")
                } else {
                    Hotbar.enter(plugin, p, s)
                }
            }
            "page" -> {
                val which = if (args.size > 1) args[1].lowercase(Locale.ROOT) else ""
                if (which != "team" && which != "map") {
                    Msg.err(p, "Usage: /$label page <team|map>")
                    return true
                }
                Hotbar.enter(plugin, p, s)
                Hotbar.setPage(plugin, p, s, if (which == "map") 1 else 0)
            }
            "facing" -> facing(p, s, label, args)
            "snap" -> {
                val v = if (args.size > 1) args[1].lowercase(Locale.ROOT) else ""
                if (v == "reset") {
                    s.snapOverride = null
                } else if (v == "off" || v == "none") {
                    s.snapOverride = 0.0
                } else {
                    val d = v.toDoubleOrNull()
                    if (d == null || d < 0) {
                        Msg.err(p, "Usage: /$label snap <degrees|off|reset>   (shops and upgrades; default 45)")
                        return true
                    }
                    s.snapOverride = d
                }
                val step = FacingPolicy.step(plugin.settings(), s)
                Msg.ok(p, "Shop/upgrade snap: " + (if (step <= 0) "off" else "$step°"))
            }
            "pitch" -> {
                val v = if (args.size > 1) args[1].lowercase(Locale.ROOT) else ""
                if (v == "reset") {
                    s.pitchOverride = null
                } else {
                    val d = v.toDoubleOrNull()
                    if (d == null) {
                        Msg.err(p, "Usage: /$label pitch <degrees|reset>   (shops and upgrades; default 0)")
                        return true
                    }
                    s.pitchOverride = d
                }
                Msg.ok(p, "Shop/upgrade pitch: " + FacingPolicy.pitch(plugin.settings(), s) + "°")
            }
            "import" -> {
                val n = ArenaImporter.importInto(plugin, p.world, s)
                if (n < 0) Msg.err(p, "No Bedwars1058 arena file found for world " + p.world.name + ".")
                else Msg.ok(p, "Loaded $n existing element(s).")
                plugin.ghosts().refresh(p, s)
            }
            "mode" -> {
                val m = if (args.size > 1) SetupMode.parse(args[1]) else null
                if (m == null) {
                    Msg.err(p, "Usage: /$label mode <manual|semi|auto>")
                    return true
                }
                s.mode = m
                Msg.ok(p, "Setup mode: " + m.label)
            }
            "gamemode" -> {
                val g = if (args.size > 1) GameModeType.parse(args[1]) else null
                if (g == null) {
                    Msg.err(p, "Usage: /$label gamemode <1v1v1v1|1v1|2v2v2v2|2v2|3v3v3v3|3v3|4v4v4v4|4v4|solos|doubles>")
                    return true
                }
                TeamManager.applyGamemode(plugin, p, s, g, null)
            }
            "status" -> {
                Msg.info(p, "Mode: " + s.mode.label + ", gamemode: " + (s.gamemode?.label ?: "none"))
                val missing = Validator.missingAll(s, plugin.settings())
                if (missing.isEmpty()) Msg.ok(p, "Nothing is missing.")
                else for (line in missing) Msg.plain(p, " - $line")
            }
            "reset" -> {
                s.reset()
                plugin.ghosts().refresh(p, s)
                Hotbar.render(plugin, p, s)
                Msg.ok(p, "Setup state cleared (this does not delete anything inside Bedwars1058).")
            }
            else -> Msg.err(p, "Usage: /$label [open|exit|tools|page|mode|gamemode|facing|snap|pitch|import|status|reset]")
        }
        return true
    }

    private fun facing(p: Player, s: SetupSession, label: String, args: Array<String>) {
        val v = if (args.size > 1) args[1].lowercase(Locale.ROOT) else ""
        if (v == "reset") {
            s.facingMode = null
        } else {
            val m = Facing.Mode.parse(v)
            val d = v.toDoubleOrNull()
            if (m != null && m != Facing.Mode.FIXED) {
                s.facingMode = m
            } else if (d != null) {
                s.facingMode = Facing.Mode.FIXED
                s.facingFixed = d
            } else {
                Msg.err(p, "Usage: /$label facing <toward|same|degrees|reset>")
                Msg.plain(p, " toward = the shop looks back at you (default); same = the way you face; degrees = a fixed yaw (0 south, 90 west, 180 north, 270 east)")
                return
            }
        }
        val m = FacingPolicy.mode(plugin.settings(), s)
        Msg.ok(
            p,
            "Shop/upgrade facing: " + when (m) {
                Facing.Mode.FIXED -> "fixed " + FacingPolicy.fixedYaw(plugin.settings(), s) + "°"
                Facing.Mode.SAME_AS_PLAYER -> "same way you face"
                Facing.Mode.TOWARD_PLAYER -> "looks back toward you"
            }
        )
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        fun opts(list: List<String>, typed: String) = list.filter { it.startsWith(typed.lowercase(Locale.ROOT)) }
        if (args.size == 1) {
            return opts(listOf("open", "exit", "tools", "page", "mode", "gamemode", "facing", "snap", "pitch", "import", "status", "reset"), args[0])
        }
        if (args.size != 2) return emptyList()
        return when (args[0].lowercase(Locale.ROOT)) {
            "mode" -> opts(listOf("manual", "semi", "auto"), args[1])
            "page" -> opts(listOf("team", "map"), args[1])
            "facing" -> opts(listOf("toward", "same", "reset"), args[1])
            "snap" -> opts(listOf("45", "22.5", "15", "90", "off", "reset"), args[1])
            "gamemode" -> opts(GameModeType.entries.map { it.label }, args[1])
            else -> emptyList()
        }
    }
}
