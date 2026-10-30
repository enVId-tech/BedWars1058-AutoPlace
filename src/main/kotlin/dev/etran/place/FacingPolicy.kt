package dev.etran.place

import dev.etran.Settings
import dev.etran.model.Element
import dev.etran.model.Facing
import dev.etran.model.Frame
import dev.etran.model.SetupSession
import org.bukkit.Location

/** Combines config.yml facing settings with the per-session overrides set by /bwsetup facing|snap|pitch. */
object FacingPolicy {
    @JvmStatic
    fun mode(st: Settings, s: SetupSession?): Facing.Mode = s?.facingMode ?: st.shopFacingMode()

    @JvmStatic
    fun fixedYaw(st: Settings, s: SetupSession?): Double =
        if (s != null && s.facingMode == Facing.Mode.FIXED) s.facingFixed else st.shopFixedYaw()

    /** Shop/upgrade yaw step in degrees (0 = no snapping). */
    @JvmStatic
    fun step(st: Settings, s: SetupSession?): Double = s?.snapOverride ?: st.shopSnapDegrees()

    @JvmStatic
    fun pitch(st: Settings, s: SetupSession?): Double = s?.pitchOverride ?: st.shopPitch()

    /**
     * The spot [at] with the facing an element gets: shops/upgrades per the facing mode (default: looking back
     * at the player) and pitch 0; spawns snapped to the spawn step; everything else the player's yaw.
     */
    @JvmStatic
    fun orient(st: Settings, s: SetupSession?, e: Element, at: Location, playerYaw: Double): Location {
        val o = at.clone()
        if (Frame.locksFacing(e)) {
            o.yaw = Facing.shopYaw(mode(st, s), fixedYaw(st, s), playerYaw, step(st, s)).toFloat()
            o.pitch = pitch(st, s).toFloat()
        } else if (e == Element.SPAWN) {
            o.yaw = Facing.snap(playerYaw, st.spawnSnapDegrees()).toFloat()
            o.pitch = 0f
        } else {
            o.yaw = playerYaw.toFloat()
            o.pitch = 0f
        }
        return o
    }
}
