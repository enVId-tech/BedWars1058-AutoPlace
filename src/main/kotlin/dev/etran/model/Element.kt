package dev.etran.model

/** Everything that has to exist for an arena to be complete. */
enum class Element(val label: String, val perTeam: Boolean) {
    // per-team
    SPAWN("Spawn", true),
    BED("Bed", true),
    SHOP("Shop", true),
    UPGRADE("Upgrade", true),
    GEN_IRON("Iron Generator", true),
    GEN_GOLD("Gold Generator", true),
    KILL_DROPS("Drop Area", true),

    // map-wide
    WAITING_SPAWN("Player (Waiting) Spawn", false),
    SPECTATOR_SPAWN("Spectator Spawn", false),
    LOBBY_POS1("Lobby Region Corner 1", false),
    LOBBY_POS2("Lobby Region Corner 2", false),
    GEN_DIAMOND("Diamond Generator", false),
    GEN_EMERALD("Emerald Generator", false);

    /** Elements whose position is captured into the base template (the bed is the origin, so it is not). */
    fun capturable(): Boolean = perTeam && this != BED

    /** Generator type string used by the Bedwars command, or null. */
    fun genType(): String? = when (this) {
        GEN_IRON -> "iron"
        GEN_GOLD -> "gold"
        GEN_DIAMOND -> "diamond"
        GEN_EMERALD -> "emerald"
        else -> null
    }

    fun isGenerator(): Boolean = genType() != null

    companion object {
        @JvmStatic
        fun teamElements(): List<Element> = entries.filter { it.perTeam }

        @JvmStatic
        fun mapElements(): List<Element> = entries.filter { !it.perTeam }

        @JvmStatic
        fun join(list: List<Element>): String = list.joinToString(", ") { it.label }
    }
}
