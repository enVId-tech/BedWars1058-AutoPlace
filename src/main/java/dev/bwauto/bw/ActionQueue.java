package dev.etran.bw;

import dev.etran.BwAutoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Runs teleports and commands one after another with a tick delay in between, replacing the Skript's
 * {@code wait 2 ticks} chains. Everything runs on the main thread.
 */
public final class ActionQueue {
    private record Step(Runnable action, int delayAfter) {
    }

    private final BwAutoPlugin plugin;
    private final Player player;
    private final Deque<Step> steps = new ArrayDeque<>();
    private Runnable onDone;
    private boolean cancelled;
    private boolean finished;

    public ActionQueue(BwAutoPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public ActionQueue then(Runnable r) {
        steps.add(new Step(r, plugin.settings().stepDelay()));
        return this;
    }

    public ActionQueue teleport(Location loc) {
        return then(() -> player.teleport(loc));
    }

    /** Runs the command as the player; a null command (disabled template) is skipped. */
    public ActionQueue command(String cmd) {
        if (cmd == null) return this;
        return then(() -> player.performCommand(cmd));
    }

    public ActionQueue pause(int ticks) {
        steps.add(new Step(() -> {
        }, Math.max(1, ticks)));
        return this;
    }

    public ActionQueue done(Runnable r) {
        this.onDone = r;
        return this;
    }

    public int size() {
        return steps.size();
    }

    public void cancel() {
        cancelled = true;
    }

    public void start() {
        next();
    }

    private void next() {
        if (cancelled || !player.isOnline()) {
            finish();
            return;
        }
        Step s = steps.poll();
        if (s == null) {
            finish();
            return;
        }
        try {
            s.action().run();
        } catch (Throwable t) {
            plugin.getLogger().warning("Setup step failed: " + t);
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::next, s.delayAfter());
    }

    private void finish() {
        if (finished) return;
        finished = true;
        if (onDone != null) {
            try {
                onDone.run();
            } catch (Throwable t) {
                plugin.getLogger().warning("Setup completion handler failed: " + t);
            }
        }
    }
}
