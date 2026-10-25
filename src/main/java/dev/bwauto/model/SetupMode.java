package dev.etran.model;

import java.util.Locale;

public enum SetupMode {
    /** ArenaSetup style: every button just runs the Bedwars command at your position. */
    MANUAL("Manual"),
    /** Skript port: capture the first base, then apply it base by base with confirmation. */
    SEMI("Semi-Automatic"),
    /** Capture the first base, then everything else (bases, lobby, generators) is algorithmic. */
    AUTO("Fully Automatic");

    public final String label;

    SetupMode(String label) {
        this.label = label;
    }

    public SetupMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public SetupMode prev() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    public static SetupMode parse(String s) {
        if (s == null) return null;
        String u = s.toUpperCase(Locale.ROOT);
        for (SetupMode m : values()) {
            if (m.name().equals(u)) return m;
        }
        if (u.startsWith("SEMI")) return SEMI;
        return null;
    }
}
