package dev.etran.scan

import dev.etran.infer.BlockGrid
import dev.etran.infer.Kind

/** Adapts a chunk-snapshot view of the world to the version-independent detection algorithms. */
class WorldGrid(private val snap: WorldSnap) : BlockGrid {
    override fun kind(x: Int, y: Int, z: Int): Kind = Mats.kind(snap.type(x, y, z))
}
