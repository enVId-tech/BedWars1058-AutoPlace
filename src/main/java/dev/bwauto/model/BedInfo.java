package dev.etran.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

/**
 * A bed identified by its foot block. {@code facingYaw} is the yaw of the foot -> head direction, which
 * is the frame orientation for the base. {@code material} keeps the bed colour / block type.
 */
public record BedInfo(String world, int x, int y, int z, float facingYaw, Material material) {

    public String key() {
        return world + "," + x + "," + y + "," + z;
    }

    public Location footCenter(World w) {
        return new Location(w, x + 0.5, y, z + 0.5);
    }

    public double distanceTo(BedInfo o) {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Resolve a bed from either of its two blocks, or null if the block is not a bed. */
    public static BedInfo fromBlock(Block b) {
        BlockData d = b.getBlockData();
        if (!(d instanceof org.bukkit.block.data.type.Bed bed)) return null;
        BlockFace facing = bed.getFacing();
        Block foot = bed.getPart() == org.bukkit.block.data.type.Bed.Part.FOOT
                ? b
                : b.getRelative(facing.getOppositeFace());
        return new BedInfo(b.getWorld().getName(), foot.getX(), foot.getY(), foot.getZ(),
                yawOf(facing), b.getType());
    }

    public static float yawOf(BlockFace f) {
        return switch (f) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
    }
}
