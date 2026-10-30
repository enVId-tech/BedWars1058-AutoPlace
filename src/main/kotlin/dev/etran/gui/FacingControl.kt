package dev.etran.gui

import dev.etran.EasySetupPlugin
import dev.etran.model.Facing
import dev.etran.model.SetupSession
import dev.etran.place.FacingPolicy
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.Locale

/** The shop/upgrade facing button: left-click cycles the facing mode, right-click the snap step, shift resets. */
internal object FacingControl {
    private val STEPS = doubleArrayOf(45.0, 22.5, 15.0, 90.0, 0.0)

    fun item(plugin: EasySetupPlugin, s: SetupSession): ItemStack {
        val m = FacingPolicy.mode(plugin.settings(), s)
        val step = FacingPolicy.step(plugin.settings(), s)
        val mode = when (m) {
            Facing.Mode.TOWARD_PLAYER -> "looks back toward you"
            Facing.Mode.SAME_AS_PLAYER -> "same way you face"
            Facing.Mode.FIXED -> "fixed " + FacingPolicy.fixedYaw(plugin.settings(), s).toInt() + "°"
        }
        return Items.of(
            "COMPASS", "&eShop & upgrade facing",
            "&7Direction: &f$mode",
            "&7Snap: &f" + (if (step <= 0) "off" else trim(step) + "°") + "&7   Pitch: &f" +
                trim(FacingPolicy.pitch(plugin.settings(), s)) + "°",
            "&eLeft-click: change direction",
            "&eRight-click: change snap",
            "&eShift-click: back to config.yml values",
            "&8Also: /bwsetup facing, snap, pitch"
        )
    }

    fun click(s: SetupSession, e: InventoryClickEvent) {
        if (e.isShiftClick) {
            s.facingMode = null
            s.snapOverride = null
            s.pitchOverride = null
            return
        }
        if (e.isRightClick) {
            val cur = s.snapOverride ?: 45.0
            var idx = 0
            for (i in STEPS.indices) if (Math.abs(STEPS[i] - cur) < 1e-9) idx = i
            s.snapOverride = STEPS[(idx + 1) % STEPS.size]
        } else {
            val cur = s.facingMode ?: Facing.Mode.TOWARD_PLAYER
            s.facingMode = Facing.Mode.entries[(cur.ordinal + 1) % Facing.Mode.entries.size]
        }
    }

    private fun trim(d: Double): String =
        if (d == Math.rint(d)) d.toLong().toString() else String.format(Locale.ROOT, "%.1f", d)
}
