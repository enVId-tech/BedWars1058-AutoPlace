package dev.etran.model;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SessionManager {
    private final Map<UUID, SetupSession> sessions = new HashMap<>();

    /** The player's session, created on first use. */
    public SetupSession get(Player p) {
        return sessions.computeIfAbsent(p.getUniqueId(), SetupSession::new);
    }

    /** The player's session, or null if they never opened the setup. */
    public SetupSession peek(Player p) {
        return sessions.get(p.getUniqueId());
    }

    public void remove(UUID id) {
        sessions.remove(id);
    }
}
