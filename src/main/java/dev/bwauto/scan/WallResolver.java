package dev.etran.scan;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Keeps generators and shops out of walls. If the computed position is inside a solid block, a ray is cast
 * from the base spawn towards it; the position is pulled back along that ray to the closest standable
 * block before the wall. The spawn itself is the last resort.
 */
public final class WallResolver {
    private WallResolver() {
    }

    public static Location resolve(Location spawn, Location target) {
        World w = target.getWorld();
        if (w == null || isFree(target)) return target;

        Vector dir = target.toVector().subtract(spawn.toVector());
        double dist = dir.length();
        if (dist > 1e-6) {
            dir.normalize();
            Location eye = spawn.clone().add(0, 1, 0);
            RayTraceResult hit = w.rayTraceBlocks(eye, dir, dist + 1.0, FluidCollisionMode.NEVER, true);
            double travel = hit == null ? dist : eye.toVector().distance(hit.getHitPosition());
            for (double t = travel - 0.3; t >= 0; t -= 0.25) {
                Location p = spawn.clone().add(dir.clone().multiply(t));
                Location c = standing(p);
                if (c != null) {
                    c.setYaw(target.getYaw());
                    c.setPitch(0f);
                    return c;
                }
            }
        }
        Location fallback = spawn.clone();
        fallback.setYaw(target.getYaw());
        fallback.setPitch(0f);
        return fallback;
    }

    /** Feet and head blocks are passable. */
    public static boolean isFree(Location l) {
        World w = l.getWorld();
        if (w == null) return false;
        Block feet = w.getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
        Block head = feet.getRelative(0, 1, 0);
        return feet.isPassable() && head.isPassable();
    }

    /** A block-centred standing position near p (same column, +-1 vertically), or null. */
    private static Location standing(Location p) {
        World w = p.getWorld();
        int x = p.getBlockX();
        int z = p.getBlockZ();
        int y0 = (int) Math.floor(p.getY() + 0.01);
        for (int dy : new int[]{0, -1, 1}) {
            int y = y0 + dy;
            Block feet = w.getBlockAt(x, y, z);
            if (feet.isPassable() && feet.getRelative(0, 1, 0).isPassable()
                    && feet.getRelative(0, -1, 0).getType().isSolid()) {
                return new Location(w, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }
}
