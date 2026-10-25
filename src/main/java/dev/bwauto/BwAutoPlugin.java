package dev.etran;

import dev.etran.bw.BwCommands;
import dev.etran.bw.CommandSniffer;
import dev.etran.cmd.AutoCommand;
import dev.etran.cmd.NextGenCommand;
import dev.etran.cmd.SetupCommand;
import dev.etran.gui.MenuListener;
import dev.etran.model.SessionManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BwAutoPlugin extends JavaPlugin {
    private Settings settings;
    private SessionManager sessions;
    private BwCommands bw;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = new Settings(this);
        this.sessions = new SessionManager();
        this.bw = new BwCommands(this);

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new CommandSniffer(this), this);

        bind("bwsetup", new SetupCommand(this));
        bind("bwauto", new AutoCommand(this));
        bind("nextgen", new NextGenCommand(this));
        getLogger().info("BedwarsAutoSetup enabled.");
    }

    private void bind(String name, TabExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Command missing from plugin.yml: " + name);
            return;
        }
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
    }

    public Settings settings() {
        return settings;
    }

    public SessionManager sessions() {
        return sessions;
    }

    public BwCommands bw() {
        return bw;
    }
}
