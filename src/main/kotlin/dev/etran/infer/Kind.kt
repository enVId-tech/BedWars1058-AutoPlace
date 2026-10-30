package dev.etran.infer

/**
 * Coarse block classes the detection algorithms care about. Keeping the algorithms on this small enum
 * (instead of `Material`) makes them version independent and testable without a server.
 */
enum class Kind {
    AIR, PASSABLE, BED, SOLID, STAIRS, SLAB, IRON_BLOCK, GOLD_BLOCK, DIAMOND_BLOCK, EMERALD_BLOCK;

    /** An entity can occupy this block (beds count: players walk over them). */
    fun passable(): Boolean = this == AIR || this == PASSABLE || this == BED

    /** Something an entity can stand on. */
    fun ground(): Boolean = !passable()

    /** Stairs and slabs: partial blocks often used to frame a generator. */
    fun partial(): Boolean = this == STAIRS || this == SLAB
}
