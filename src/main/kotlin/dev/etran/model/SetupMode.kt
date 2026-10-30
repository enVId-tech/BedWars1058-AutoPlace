package dev.etran.model

import java.util.Locale

enum class SetupMode(val label: String) {
    /** ArenaSetup style: every button just runs the Bedwars command at your position. */
    MANUAL("Manual"),

    /** Skript port: capture the first base, then apply it base by base with confirmation. */
    SEMI("Semi-Automatic"),

    /** Capture the first base, then everything else (bases, lobby, generators) is algorithmic. */
    AUTO("Fully Automatic");

    fun next(): SetupMode = entries[(ordinal + 1) % entries.size]

    fun prev(): SetupMode = entries[(ordinal + entries.size - 1) % entries.size]

    companion object {
        @JvmStatic
        fun parse(s: String?): SetupMode? {
            if (s == null) return null
            val u = s.uppercase(Locale.ROOT)
            for (m in entries) if (m.name == u) return m
            return if (u.startsWith("SEMI")) SEMI else null
        }
    }
}
