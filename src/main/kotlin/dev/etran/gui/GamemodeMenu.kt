package dev.etran.gui

import dev.etran.EasySetupPlugin
import dev.etran.model.GameModeType
import dev.etran.place.TeamManager
import org.bukkit.entity.Player

class GamemodeMenu(plugin: EasySetupPlugin, player: Player) : Menu(plugin, player) {
    override fun title(): String = "Select Gamemode"

    override fun rows(): Int = 4

    override fun render() {
        fill(Items.GRAY_PANE)
        for (i in ORDER.indices) {
            val g = ORDER[i]
            val current = g == session.gamemode
            set(
                SLOTS[i],
                Items.of(
                    g.icon, (if (current) "&a▶ " else "&f") + g.label,
                    "&7" + g.teams + " teams of " + g.size + " player" + (if (g.size == 1) "" else "s"),
                    if (current) "&aCurrently selected" else "&eClick to select"
                )
            ) {
                if (g != session.gamemode) TeamManager.applyGamemode(plugin, player, session, g) { refresh() }
            }
        }
        set(27, Items.of(Items.RED_CONCRETE, "&c&lBack")) { go(MainMenu(plugin, player)) }
    }

    private companion object {
        // Left: four-team (row 1) and two-team (row 2) modes; right: eight-team Solos / Doubles.
        val ORDER = arrayOf(
            GameModeType.T1V1V1V1, GameModeType.T2V2V2V2, GameModeType.T3V3V3V3, GameModeType.T4V4V4V4,
            GameModeType.T1V1, GameModeType.T2V2, GameModeType.T3V3, GameModeType.T4V4,
            GameModeType.SOLOS, GameModeType.DOUBLES
        )
        val SLOTS = intArrayOf(10, 11, 12, 13, 19, 20, 21, 22, 15, 24)
    }
}
