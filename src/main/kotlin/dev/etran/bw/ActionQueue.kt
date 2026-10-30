package dev.etran.bw

import dev.etran.EasySetupPlugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.ArrayDeque

/**
 * A sequence of actions (teleports, commands, pauses) spread over server ticks, so Bedwars1058 sees each
 * command with the player where it expects them.
 */
class ActionQueue(private val plugin: EasySetupPlugin, private val player: Player) {
    private class Step(val action: Runnable, val delayAfter: Int)

    private val steps = ArrayDeque<Step>()
    private var onDone: Runnable? = null
    private var cancelled = false
    private var finished = false

    fun then(r: Runnable): ActionQueue {
        steps.add(Step(r, plugin.settings().stepDelay()))
        return this
    }

    fun teleport(loc: Location): ActionQueue = then(Runnable { player.teleport(loc) })

    /** Runs the command as the player; a null command (disabled template) is skipped. */
    fun command(cmd: String?): ActionQueue {
        if (cmd == null) return this
        return then(Runnable { player.performCommand(cmd) })
    }

    fun pause(ticks: Int): ActionQueue {
        steps.add(Step(Runnable { }, maxOf(1, ticks)))
        return this
    }

    fun done(r: Runnable): ActionQueue {
        onDone = r
        return this
    }

    fun size(): Int = steps.size

    fun cancel() {
        cancelled = true
    }

    fun start() = next()

    private fun next() {
        if (cancelled || !player.isOnline) {
            finish()
            return
        }
        val s = steps.poll()
        if (s == null) {
            finish()
            return
        }
        try {
            s.action.run()
        } catch (t: Throwable) {
            plugin.logger.warning("Setup step failed: $t")
        }
        Bukkit.getScheduler().runTaskLater(plugin, { next() }, s.delayAfter.toLong())
    }

    private fun finish() {
        if (finished) return
        finished = true
        try {
            onDone?.run()
        } catch (t: Throwable) {
            plugin.logger.warning("Setup completion handler failed: $t")
        }
    }
}
