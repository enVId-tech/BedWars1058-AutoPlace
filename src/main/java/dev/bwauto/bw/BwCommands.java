package dev.etran.bw;

import dev.etran.BwAutoPlugin;

/** Builds Bedwars1058 setup command lines from the templates in config.yml. */
public final class BwCommands {
    private final BwAutoPlugin plugin;

    public BwCommands(BwAutoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @param key config key under bw-commands
     * @param kv  alternating placeholder names and values, e.g. {@code "team", "Red"}
     * @return the command without a leading slash, or null if the template is empty/disabled
     */
    public String build(String key, String... kv) {
        String t = plugin.settings().commandTemplate(key);
        if (t == null || t.isBlank()) return null;
        t = t.replace("{root}", plugin.settings().bwRoot());
        for (int i = 0; i + 1 < kv.length; i += 2) {
            t = t.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        t = t.replaceAll("\\s+", " ").trim();
        return t.startsWith("/") ? t.substring(1) : t;
    }
}
