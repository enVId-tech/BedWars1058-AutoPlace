package dev.etran.model

import org.bukkit.entity.Player
import java.util.UUID

class SessionManager {
    private val sessions = HashMap<UUID, SetupSession>()

    /** The player's session, created on first use. */
    fun get(p: Player): SetupSession = sessions.getOrPut(p.uniqueId) { SetupSession(p.uniqueId) }

    /** The player's session, or null if they never opened the setup. */
    fun peek(p: Player): SetupSession? = sessions[p.uniqueId]

    /** A snapshot of every session. */
    fun all(): List<SetupSession> = ArrayList(sessions.values)

    fun remove(id: UUID) {
        sessions.remove(id)
    }
}
