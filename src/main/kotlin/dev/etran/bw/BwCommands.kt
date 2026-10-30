package dev.etran.bw

import dev.etran.EasySetupPlugin

/** Builds Bedwars1058 setup command lines from the templates in config.yml. */
class BwCommands(private val plugin: EasySetupPlugin) {
    /**
     * @param key config key under bw-commands
     * @param kv alternating placeholder names and values, e.g. `"team", "Red"`
     * @return the command without a leading slash, or null if the template is empty/disabled
     */
    fun build(key: String, vararg kv: String): String? {
        var t = plugin.settings().commandTemplate(key)
        if (t == null || t.trim().isEmpty()) return null
        t = t.replace("{root}", plugin.settings().bwRoot())
        var i = 0
        while (i + 1 < kv.size) {
            t = t!!.replace("{" + kv[i] + "}", kv[i + 1])
            i += 2
        }
        t = t!!.replace(Regex("\\s+"), " ").trim()
        return if (t.startsWith("/")) t.substring(1) else t
    }
}
