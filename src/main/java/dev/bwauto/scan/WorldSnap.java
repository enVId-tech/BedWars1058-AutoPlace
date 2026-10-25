package dev.etran.scan;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

/** Fast read-only block access over a rectangle of already-generated chunks. */
public final class WorldSnap {
    private final Map<Long, ChunkSnapshot> chunks = new HashMap<>();
    public final int minY;
    public final int maxY; // exclusive

    public WorldSnap(World w, int minX, int minZ, int maxX, int maxZ) {
        this.minY = w.getMinHeight();
        this.maxY = w.getMaxHeight();
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                if (w.isChunkGenerated(cx, cz)) {
                    chunks.put(key(cx, cz), w.getChunkAt(cx, cz).getChunkSnapshot(false, false, false));
                }
            }
        }
    }

    public Material type(int x, int y, int z) {
        if (y < minY || y >= maxY) return Material.AIR;
        ChunkSnapshot s = chunks.get(key(x >> 4, z >> 4));
        if (s == null) return Material.AIR;
        return s.getBlockType(x & 15, y, z & 15);
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }
}
