package dev.etran

import dev.etran.bw.BwCommands
import dev.etran.bw.CommandSniffer
import dev.etran.cmd.AutoCommand
import dev.etran.cmd.NextGenCommand
import dev.etran.cmd.SetupCommand
import dev.etran.gui.MenuListener
import dev.etran.model.SessionManager
import dev.etran.tools.Ghosts
import dev.etran.tools.Hotbar
import dev.etran.tools.ToolListener
import org.bukkit.Bukkit
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Entity
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import org.bukkit.plugin.java.JavaPlugin

/** Entry point: wires settings, sessions, listeners and commands. */
class EasySetupPlugin : JavaPlugin() {
    private lateinit var settingsRef: Settings
    private lateinit var sessionsRef: SessionManager
    private lateinit var bwRef: BwCommands
    private lateinit var ghostsRef: Ghosts

    override fun onEnable() {
        saveDefaultConfig()
        settingsRef = Settings(this)
        sessionsRef = SessionManager()
        bwRef = BwCommands(this)
        ghostsRef = Ghosts(this)

        server.pluginManager.registerEvents(MenuListener(), this)
        server.pluginManager.registerEvents(ToolListener(this), this)
        registerEntitiesLoad()
        Bukkit.getScheduler().runTask(this, Runnable { ghostsRef.start() })
        server.pluginManager.registerEvents(CommandSniffer(this), this)

        bind("bwsetup", SetupCommand(this))
        bind("bwauto", AutoCommand(this))
        bind("nextgen", NextGenCommand(this))
        logger.info("BedwarsAutoSetup enabled.")
    }

    override fun onDisable() {
        if (::sessionsRef.isInitialized) {
            for (s in sessionsRef.all()) {
                if (!s.toolsActive) continue
                val p = Bukkit.getPlayer(s.owner)
                if (p != null) Hotbar.deactivate(this, p, s)
            }
        }
        if (::ghostsRef.isInitialized) ghostsRef.stop()
    }

    /** 1.17+ loads entities separately from chunks; hook that event by name so older servers are unaffected. */
    @Suppress("UNCHECKED_CAST")
    private fun registerEntitiesLoad() {
        try {
            val c = Class.forName("org.bukkit.event.world.EntitiesLoadEvent") as Class<out Event>
            val get = c.getMethod("getEntities")
            server.pluginManager.registerEvent(c, object : Listener {}, EventPriority.NORMAL, EventExecutor { _, event ->
                try {
                    ghostsRef.sweepEntities(get.invoke(event) as Collection<Entity>)
                } catch (ignored: Throwable) {
                    // best effort
                }
            }, this)
        } catch (ignored: Throwable) {
            // not available on this version
        }
    }

    private fun bind(name: String, executor: TabExecutor) {
        val cmd = getCommand(name)
        if (cmd == null) {
            logger.severe("Command missing from plugin.yml: $name")
            return
        }
        cmd.executor = executor
        cmd.tabCompleter = executor
    }

    fun settings(): Settings = settingsRef

    fun sessions(): SessionManager = sessionsRef

    fun ghosts(): Ghosts = ghostsRef

    fun bw(): BwCommands = bwRef
}
