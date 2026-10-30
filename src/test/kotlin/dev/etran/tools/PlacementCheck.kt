package dev.etran.tools

import dev.etran.model.ArenaFormat
import dev.etran.model.Element
import dev.etran.model.Facing
import dev.etran.model.SetupSession
import dev.etran.model.TeamColor
import dev.etran.model.TeamState
import org.bukkit.Location
import org.bukkit.World
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.math.abs
import kotlin.system.exitProcess

/**
 * Standalone check of the placement logic that needs no server: shop facing maths, reading the Bedwars1058
 * arena file, and finding/forgetting elements for deletion. Run with `./gradlew placementCheck`.
 */
object PlacementCheck {
    private var failures = 0

    private fun check(ok: Boolean, what: String) {
        println((if (ok) "PASS " else "FAIL ") + what)
        if (!ok) failures++
    }

    private fun eq(a: Double, b: Double) = abs(a - b) < 1e-6

    private fun fakeWorld(): World =
        Proxy.newProxyInstance(World::class.java.classLoader, arrayOf<Class<*>>(World::class.java)) { proxy, m, args ->
            when (m.name) {
                "equals" -> proxy === args[0]
                "hashCode" -> System.identityHashCode(proxy)
                "getName" -> "fake"
                else -> null
            }
        } as World

    @JvmStatic
    fun main(args: Array<String>) {
        facing()
        arenaFormat()
        removal()
        println(if (failures == 0) "ALL PASS" else "$failures FAILURE(S)")
        exitProcess(if (failures == 0) 0 else 1)
    }

    private fun facing() {
        // Player facing south (yaw 0) places a shop: it must look back north (180), not at the wall behind it.
        check(eq(Facing.shopYaw(Facing.Mode.TOWARD_PLAYER, 0.0, 0.0, 45.0), 180.0), "toward: player facing south -> shop faces north")
        check(eq(Facing.shopYaw(Facing.Mode.TOWARD_PLAYER, 0.0, 90.0, 45.0), 270.0), "toward: player facing west -> shop faces east")
        check(eq(Facing.shopYaw(Facing.Mode.TOWARD_PLAYER, 0.0, -90.0, 45.0), 90.0), "toward: negative yaw is normalised (-90 -> 90)")
        check(eq(Facing.shopYaw(Facing.Mode.TOWARD_PLAYER, 0.0, 100.0, 45.0), 270.0), "toward: 100 -> 280 snaps to 270")
        check(eq(Facing.shopYaw(Facing.Mode.TOWARD_PLAYER, 0.0, 130.0, 45.0), 315.0), "toward: 130 -> 310 snaps to 315 (a diagonal)")
        check(eq(Facing.shopYaw(Facing.Mode.SAME_AS_PLAYER, 0.0, 100.0, 45.0), 90.0), "same: 100 snaps to 90")
        check(eq(Facing.shopYaw(Facing.Mode.FIXED, 135.0, 12.0, 45.0), 135.0), "fixed: ignores the player")
        check(eq(Facing.shopYaw(Facing.Mode.TOWARD_PLAYER, 0.0, 10.0, 0.0), 190.0), "snap 0: exact yaw kept (10 -> 190)")
        check(eq(Facing.shopYaw(Facing.Mode.TOWARD_PLAYER, 0.0, 10.0, 22.5), 180.0), "snap 22.5 honoured (190 -> 180)")
        var on45 = true
        var y = -720
        while (y <= 720) {
            val r = Facing.shopYaw(Facing.Mode.TOWARD_PLAYER, 0.0, y.toDouble(), 45.0)
            if (abs(r / 45.0 - Math.rint(r / 45.0)) > 1e-9 || r < 0 || r >= 360) on45 = false
            y += 7
        }
        check(on45, "every result is a multiple of 45 in [0,360)")
        check(
            Facing.Mode.parse("toward") == Facing.Mode.TOWARD_PLAYER && Facing.Mode.parse("same") == Facing.Mode.SAME_AS_PLAYER &&
                Facing.Mode.parse("nonsense") == null,
            "mode parsing"
        )
    }

    private fun arenaFormat() {
        val a = ArenaFormat.parseLocation("10.5,64,-5.5,90.0,0.0")
        check(a != null && eq(a[0], 10.5) && eq(a[1], 64.0) && eq(a[2], -5.5) && eq(a[3], 90.0) && eq(a[4], 0.0), "parse x,y,z,yaw,pitch")
        val b = ArenaFormat.parseLocation("world,1,2,3")
        check(b != null && eq(b[0], 1.0) && eq(b[2], 3.0) && eq(b[3], 0.0), "parse with a world prefix")
        check(ArenaFormat.parseLocation("RED") == null && ArenaFormat.parseLocation("1,2") == null, "non-locations rejected")
        val cases = listOf(
            Triple("Team.Red.Shop", "RED", Element.SHOP), Triple("Team.Blue.Upgrade", "BLUE", Element.UPGRADE),
            Triple("Team.Green.Spawn", "GREEN", Element.SPAWN), Triple("Team.Yellow.Bed", "YELLOW", Element.BED),
            Triple("Team.Aqua.Iron", "AQUA", Element.GEN_IRON), Triple("Team.Pink.Gold", "PINK", Element.GEN_GOLD),
            Triple("Team.White.KillDrops", "WHITE", Element.KILL_DROPS), Triple("generator.Diamond", null, Element.GEN_DIAMOND),
            Triple("generator.Emerald", null, Element.GEN_EMERALD), Triple("waiting.Pos1", null, Element.LOBBY_POS1),
            Triple("waiting.Pos2", null, Element.LOBBY_POS2), Triple("waiting.Loc", null, Element.WAITING_SPAWN),
            Triple("Spectator", null, Element.SPECTATOR_SPAWN)
        )
        for ((path, team, el) in cases) {
            val s = ArenaFormat.classify(path)
            check(s != null && s.element == el && s.team == team, "classify $path")
        }
        check(ArenaFormat.classify("Team.Red.Color") == null && ArenaFormat.classify("maxInTeam") == null, "unrelated keys ignored")
    }

    private fun removal() {
        val w = fakeWorld()
        val s = SetupSession(UUID.randomUUID())
        val red = s.team(TeamColor.RED)
        red.created = true
        red.pos[Element.SHOP] = Location(w, 10.5, 64.0, 10.5, 180f, 0f)
        red.placed.add(Element.SHOP)
        red.gens.add(TeamState.GenRecord("iron", Location(w, 20.5, 64.0, 20.5)))
        red.gens.add(TeamState.GenRecord("gold", Location(w, 20.5, 64.0, 20.5)))
        red.placed.add(Element.GEN_IRON)
        red.placed.add(Element.GEN_GOLD)
        s.setMap(Element.WAITING_SPAWN, Location(w, 0.5, 70.0, 0.5))

        val near = Remover.find(s, Location(w, 10.5, 64.0, 10.5), null)
        check(near.size == 1 && near[0].element == Element.SHOP, "find: the shop at the click")
        check(Remover.find(s, Location(w, 13.5, 64.0, 10.5), null).isEmpty(), "find: nothing 3 blocks away")
        check(Remover.find(s, Location(w, 10.5, 70.0, 10.5), null).isEmpty(), "find: nothing 6 blocks above")
        check(Remover.find(s, Location(w, 20.5, 64.0, 20.5), null).size == 2, "find: overlapping iron + gold both found")
        check(Remover.find(s, Location(w, 20.5, 64.0, 20.5), Element.GEN_GOLD).size == 1, "find: filtered to the held tool's element")
        check(Remover.find(s, Location(w, 0.5, 70.0, 0.5), null)[0].element == Element.WAITING_SPAWN, "find: map-wide element")

        s.template.team = TeamColor.RED
        Remover.forget(s, near[0])
        check(red.pos[Element.SHOP] == null && !red.placed.contains(Element.SHOP), "forget: shop removed from the session")
        Remover.forget(s, Remover.find(s, Location(w, 20.5, 64.0, 20.5), Element.GEN_IRON)[0])
        check(
            red.gens.size == 1 && !red.placed.contains(Element.GEN_IRON) && red.placed.contains(Element.GEN_GOLD),
            "forget: removing iron leaves the gold generator at the same spot"
        )
        Remover.forget(s, Remover.find(s, Location(w, 0.5, 70.0, 0.5), null)[0])
        check(!s.mapPlaced(Element.WAITING_SPAWN), "forget: map element removed")
    }
}
