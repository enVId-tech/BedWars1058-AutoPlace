package dev.etran.place;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.bw.ElementPlacer;
import dev.etran.model.Element;
import dev.etran.model.SetupSession;
import dev.etran.scan.GeneratorScanner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Port of the Skript's /nextgen.
 *
 * <p>With a type: find the closest unplaced diamond/emerald generator, teleport there, count down (any
 * movement cancels), place it, and repeat. When nothing is detected - or no type is given - fall back to
 * rotating the player's position around the map centre, now by any angle (default 90 degrees).
 */
public final class NextGen {
    private NextGen() {
    }

    public static void run(BwAutoPlugin plugin, Player p, String type, double degrees) {
        SetupSession s = plugin.sessions().get(p);
        if (type != null) {
            World w = p.getWorld();
            List<Location> found = GeneratorScanner.findUnplaced(w, p.getLocation(), type, plugin.settings(), s);
            if (!found.isEmpty()) {
                Location target = found.get(0);
                p.teleport(target);
                int secs = plugin.settings().countdownSeconds();
                Msg.ok(p, "Teleported to the closest unplaced " + type + " generator. Placing in " + secs
                        + " seconds... (move to cancel)");
                countdown(plugin, p, type, degrees, p.getLocation().clone(), secs);
                return;
            }
            Msg.info(p, "No unplaced " + type + " generators detected via scan. Rotating by " + fmt(degrees)
                    + " degrees instead...");
        }
        rotate(plugin, p, degrees);
    }

    private static void countdown(BwAutoPlugin plugin, Player p, String type, double degrees, Location start,
                                  int seconds) {
        new BukkitRunnable() {
            int left = seconds;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    cancel();
                    return;
                }
                if (!p.getWorld().equals(start.getWorld()) || p.getLocation().distance(start) > 0.5) {
                    Msg.err(p, "Movement detected! Setup cancelled.");
                    cancel();
                    return;
                }
                if (--left > 0) return;
                cancel();
                Element el = type.equals("diamond") ? Element.GEN_DIAMOND : Element.GEN_EMERALD;
                SetupSession s = plugin.sessions().get(p);
                ElementPlacer.placeHere(plugin, p, s, null, el, () -> {
                    Msg.ok(p, (type.equals("diamond") ? "Diamond" : "Emerald") + " generator placed!");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> NextGen.run(plugin, p, type, degrees), 5L);
                });
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /** Rotate the player's position and yaw around the map centre by {@code degrees}. */
    public static void rotate(BwAutoPlugin plugin, Player p, double degrees) {
        double cx = plugin.settings().centerX();
        double cz = plugin.settings().centerZ();
        Location l = p.getLocation();
        double rad = Math.toRadians(degrees);
        double dx = l.getX() - cx;
        double dz = l.getZ() - cz;
        // 90 degrees maps (x, z) -> (-z, x), exactly as the Skript did.
        double nx = cx + dx * Math.cos(rad) - dz * Math.sin(rad);
        double nz = cz + dx * Math.sin(rad) + dz * Math.cos(rad);
        if (plugin.settings().snap() && Math.abs(degrees % 90.0) < 1e-9) {
            nx = Math.round(nx * 2.0) / 2.0;
            nz = Math.round(nz * 2.0) / 2.0;
        }
        Location out = new Location(l.getWorld(), nx, l.getY(), nz,
                (float) dev.etran.model.Frame.norm360(l.getYaw() + degrees), 0f);
        if (out.getBlock().getType().isSolid()) out.add(0, 1, 0);
        p.teleport(out);
        Msg.ok(p, "Rotated " + fmt(degrees) + " degrees around the map centre (" + fmt(cx) + ", " + fmt(cz) + ").");
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format("%.2f", d);
    }
}
