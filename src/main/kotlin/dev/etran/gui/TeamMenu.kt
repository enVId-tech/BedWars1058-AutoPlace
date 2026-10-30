package dev.etran.gui

import dev.etran.EasySetupPlugin
import dev.etran.Msg
import dev.etran.place.TeamManager
import dev.etran.place.Validator
import dev.etran.tools.Hotbar
import org.bukkit.entity.Player

/** Team selection (what the hotbar tools place for) and team creation. Opened from the hotbar's team item. */
class TeamMenu(plugin: EasySetupPlugin, player: Player) : Menu(plugin, player) {
    override fun title(): String = "Choose Team"

    override fun rows(): Int = 3

    override fun render() {
        fill(Items.GRAY_PANE)
        val req = session.requiredTeams()
        if (req.isEmpty()) {
            set(13, Items.of("NETHER_STAR", "&6Pick a gamemode first", "&7The gamemode decides which teams exist.")) {
                go(GamemodeMenu(plugin, player))
            }
        }
        val base = if (req.size > 4) 10 else if (req.size > 2) 11 else 12
        for (i in 0 until minOf(req.size, 8)) {
            val c = req[i]
            val isSel = c == session.selected
            val exists = session.hasTeam(c)
            set(
                base + i,
                Items.of(
                    c.woolSpec(), (if (isSel) "&a▶ " else "") + c.chat + c.display,
                    if (exists) "&7Elements missing: &f" + Validator.missingForTeam(session, c).size else "&cTeam not created yet",
                    if (isSel) "&aSelected" else "&eClick to select"
                )
            ) {
                session.selected = c
                Hotbar.render(plugin, player, session)
                if (!session.hasTeam(c)) {
                    Msg.info(player, c.display + " does not exist yet - use the red concrete to create the teams.")
                }
                refresh()
            }
        }
        val missing = req.count { !session.hasTeam(it) }
        set(
            9,
            Items.of(
                Items.RED_CONCRETE, "&c&lAuto-Create Teams",
                "&7Creates every team the gamemode needs.",
                if (session.gamemode == null) "&cPick a gamemode first."
                else if (missing == 0) "&aAll teams exist." else "&eMissing: $missing of ${req.size}"
            )
        ) {
            TeamManager.autoCreate(plugin, player, session) {
                Hotbar.render(plugin, player, session)
                refresh()
            }
        }
        set(
            22,
            Items.of(
                Items.WHITE_WOOL, "&6Detect team colour from wool",
                "&7Stand at a base: picks the team whose",
                "&7colour the wool around it matches."
            )
        ) {
            TeamManager.detectWoolAt(plugin, player, session) {
                Hotbar.render(plugin, player, session)
                refresh()
            }
        }
        set(18, Items.of(Items.RED_CONCRETE, "&c&lBack")) { go(MainMenu(plugin, player)) }
        set(25, Items.of(Items.LIME_CONCRETE, "&a&lAdvanced Team & Base Setup ➜")) {
            if (session.gamemode == null) {
                Msg.err(player, "Pick a gamemode first.")
            } else {
                if (session.selected == null) session.selected = session.requiredTeams()[0]
                go(BaseMenu(plugin, player))
            }
        }
    }
}
