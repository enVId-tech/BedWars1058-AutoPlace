package dev.etran;

import dev.etran.model.Element;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Typed access to config.yml. */
public final class Settings {
    private final BwAutoPlugin plugin;

    public Settings(BwAutoPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration c() {
        return plugin.getConfig();
    }

    public String bwRoot() {
        return c().getString("bw-commands.root", "bw");
    }

    public String commandTemplate(String key) {
        return c().getString("bw-commands." + key, "");
    }

    public List<String> commandRoots() {
        List<String> raw = c().getStringList("command-roots");
        List<String> out = new ArrayList<>();
        for (String s : raw) out.add(s.toLowerCase(Locale.ROOT));
        if (out.isEmpty()) out.add("bw");
        return out;
    }

    public int stepDelay() {
        return Math.max(1, c().getInt("timing.step-delay-ticks", 2));
    }

    public int countdownSeconds() {
        return Math.max(1, c().getInt("timing.countdown-seconds", 3));
    }

    public double centerX() {
        return c().getDouble("center.x", 0.0);
    }

    public double centerZ() {
        return c().getDouble("center.z", 0.0);
    }

    public boolean snap() {
        return c().getBoolean("snap-to-half-block", true);
    }

    public int templateBedRadius() {
        return c().getInt("beds.template-search-radius", 30);
    }

    public int bedScanRadius() {
        return c().getInt("beds.scan-radius", 200);
    }

    public double bedArmorStandRadius() {
        return c().getDouble("beds.armor-stand-radius", 7.0);
    }

    public double assignedBedRadius() {
        return c().getDouble("beds.assigned-radius", 10.0);
    }

    public int lobbyRadius() {
        return Math.max(8, c().getInt("lobby-scan.radius", 48));
    }

    public int lobbyMinBlocks() {
        return c().getInt("lobby-scan.min-blocks", 24);
    }

    public double lobbyMaxOffset() {
        return c().getDouble("lobby-scan.max-center-offset", 32.0);
    }

    public int lobbyPadding() {
        return Math.max(0, c().getInt("lobby-scan.padding", 1));
    }

    public int woolRadius() {
        return Math.max(4, c().getInt("wool.radius", 24));
    }

    public int woolMinScore() {
        return c().getInt("wool.min-score", 6);
    }

    public List<String> woolSuffixes() {
        List<String> l = c().getStringList("wool.suffixes");
        if (l.isEmpty()) l = List.of("_WOOL");
        return l;
    }

    /** auto | off | force */
    public String mirrorMode() {
        return c().getString("mirror.mode", "auto").toLowerCase(Locale.ROOT);
    }

    public int mirrorRadius() {
        return Math.max(4, c().getInt("mirror.sample-radius", 14));
    }

    public double mirrorMargin() {
        return c().getDouble("mirror.margin", 0.03);
    }

    public int generatorScanRadius() {
        return Math.max(8, c().getInt("generator-scan.radius", 100));
    }

    public double defaultRotation() {
        return c().getDouble("rotation.default-degrees", 90.0);
    }

    public List<Element> requiredMapElements() {
        List<Element> out = new ArrayList<>();
        for (String s : c().getStringList("validation.required-map-elements")) {
            try {
                Element e = Element.valueOf(s.toUpperCase(Locale.ROOT));
                if (!e.perTeam) out.add(e);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Unknown element in validation.required-map-elements: " + s);
            }
        }
        return out;
    }

    public String incompleteAutoMessage(String elements) {
        String t = c().getString("messages.incomplete-auto",
                "You are attempting to run automatic placement without all game elements. "
                        + "You are missing [{elements}]. Run /bwauto confirm/deny to continue anyway.");
        return t.replace("{elements}", elements);
    }
}
