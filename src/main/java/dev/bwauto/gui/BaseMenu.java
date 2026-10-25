package dev.etran.gui;

import dev.etran.BwAutoPlugin;
import dev.etran.Msg;
import dev.etran.bw.ElementPlacer;
import dev.etran.model.BedInfo;
import dev.etran.model.Element;
import dev.etran.model.Offset;
import dev.etran.model.Template;
import dev.etran.model.TeamColor;
import dev.etran.place.Validator;
import dev.etran.scan.BedScanner;
import dev.etran.scan.WoolDetector;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Team & base setup: pick a team, place its elements, and (semi/auto) copy the base elsewhere. */
public final class BaseMenu extends Menu {
    private static final Element[] ELEMENTS = {
            Element.SPAWN, Element.BED, Element.SHOP, Element.UPGRADE,
            Element.GEN_IRON, Element.GEN_GOLD, Element.KILL_DROPS};
    private static final int[] SLOTS = {19, 20, 21, 22, 23, 24, 25};

    /** Where the "finish" button sits normally, and where it is moved to while elements are missing. */
    private static final int NEXT_SLOT_COMPLETE = 53;
    private static final int NEXT_SLOT_INCOMPLETE = 51;

    public BaseMenu(BwAutoPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Team & Base Setup";
    }

    @Override
    protected int rows() {
        return 6;
    }

    private Material icon(Element e, TeamColor team) {
        return switch (e) {
            case SPAWN -> Material.ENDER_PEARL;
            case BED -> team == null ? Material.RED_BED : team.bed();
            case SHOP -> Material.EMERALD;
            case UPGRADE -> Material.BEACON;
            case GEN_IRON -> Material.IRON_INGOT;
            case GEN_GOLD -> Material.GOLD_INGOT;
            case KILL_DROPS -> Material.DROPPER;
            default -> Material.PAPER;
        };
    }

    private String hint(Element e) {
        return switch (e) {
            case SPAWN -> "Team spawn point.";
            case BED -> "Origin of the base: every other position is saved relative to it.";
            case SHOP -> "Item shop NPC.";
            case UPGRADE -> "Team upgrades NPC.";
            case GEN_IRON -> "Iron generator (replaces the one placed before).";
            case GEN_GOLD -> "Gold generator (replaces the one placed before).";
            case KILL_DROPS -> "Where a dead player's items drop.";
            default -> "";
        };
    }

    @Override
    protected void render() {
        fill(Material.GRAY_STAINED_GLASS_PANE);
        List<TeamColor> req = session.requiredTeams();
        TeamColor sel = session.selected;

        // Team tabs
        for (int i = 0; i < req.size() && i < 4; i++) {
            TeamColor c = req.get(i);
            boolean isSel = c == sel;
            boolean exists = session.hasTeam(c);
            int missing = Validator.missingForTeam(session, c).size();
            set(2 + i, Items.of(c.wool(), (isSel ? "&a▶ " : "") + c.chat + c.display,
                    exists ? "&7Elements missing: &f" + missing : "&cTeam not created yet",
                    isSel ? "&aSelected" : "&eClick to select"),
                    e -> {
                        session.selected = c;
                        refresh();
                    });
        }

        // Element buttons
        for (int i = 0; i < ELEMENTS.length; i++) {
            Element el = ELEMENTS[i];
            boolean placed = sel != null && session.teamPlaced(sel, el);
            set(SLOTS[i], Items.of(icon(el, sel), (placed ? "&a✔ " : "&c✘ ") + el.label,
                    "&7" + hint(el), "&eClick to place at your position"),
                    e -> place(List.of(el)));
        }

        set(29, Items.of(Material.WHITE_WOOL, "&6Detect team colour from wool",
                "&7Compares wool around every bed, ignoring",
                "&7wool shared by all bases, and selects the",
                "&7colour of the base you are standing at."),
                e -> detectWool());

        set(31, Items.of(Material.FURNACE, "&6Forge combo",
                "&7Iron generator, gold generator and drop",
                "&7area at your position, in one click."),
                e -> place(List.of(Element.GEN_IRON, Element.GEN_GOLD, Element.KILL_DROPS)));

        MainMenu.automationButton(this, 33, plugin, player, session.mode);

        set(40, Items.of(Material.BOOK, "&eBase template", templateLore()), null);

        // Navigation. The "finish" button moves while anything is missing so it cannot be hit by habit.
        set(45, Items.of(Material.RED_CONCRETE, "&c&lBack"), e -> go(new MainMenu(plugin, player)));

        List<String> missing = Validator.missingAll(session, plugin.settings());
        boolean complete = missing.isEmpty();
        List<String> lore = new ArrayList<>();
        if (complete) {
            lore.add("&7Everything is placed.");
        } else {
            lore.add("&cNot everything is placed yet:");
            for (int i = 0; i < missing.size() && i < 6; i++) lore.add("&7- " + missing.get(i));
            if (missing.size() > 6) lore.add("&7... and " + (missing.size() - 6) + " more");
            lore.add("&7(Button moved to avoid a misclick.)");
        }
        set(complete ? NEXT_SLOT_COMPLETE : NEXT_SLOT_INCOMPLETE,
                Items.of(Material.LIME_CONCRETE, "&a&lFinish & Validate ➜", lore),
                e -> go(new ValidationMenu(plugin, player)));
    }

    private void place(List<Element> elems) {
        TeamColor sel = session.selected;
        ElementPlacer.placeMany(plugin, player, session, sel, elems, this::refresh);
    }

    private List<String> templateLore() {
        Template t = session.template;
        List<String> l = new ArrayList<>();
        if (session.mode == dev.etran.model.SetupMode.MANUAL) {
            l.add("&7Manual mode does not save deltas.");
            return l;
        }
        l.add("&7Team: &f" + (t.team() == null ? "none yet" : t.team().display));
        BedInfo b = t.bed();
        l.add(b == null ? "&cNo origin bed yet" : "&7Origin bed: &f" + b.x() + ", " + b.y() + ", " + b.z()
                + " &7facing &f" + (int) b.facingYaw() + "°");
        for (Element e : ELEMENTS) {
            if (!e.capturable()) continue;
            Offset o = t.offset(e);
            if (o != null) {
                l.add(String.format("&a✔ &7%s: right %.1f up %.1f fwd %.1f yaw %+d°",
                        e.label, o.right(), o.up(), o.fwd(), (int) Math.round(o.dyaw())));
            } else if (t.captured(e)) {
                l.add("&e• &7" + e.label + ": captured, waiting for bed");
            } else {
                l.add("&c✘ &7" + e.label);
            }
        }
        return l;
    }

    private void detectWool() {
        World w = player.getWorld();
        Location here = player.getLocation();
        BedInfo near = BedScanner.nearest(w, here, 60);
        if (near == null) {
            Msg.err(player, "No bed within 60 blocks. Stand at a base first.");
            return;
        }
        Location centre = new Location(w, plugin.settings().centerX(), here.getY(), plugin.settings().centerZ());
        List<BedInfo> beds = BedScanner.scan(w, centre, plugin.settings().bedScanRadius());
        WoolDetector.Result r = WoolDetector.detect(w, beds, plugin.settings(), session.requiredTeams());
        TeamColor c = r.byBed().get(near.key());
        if (c == null) {
            Msg.err(player, "Could not isolate a team colour here (not enough wool that differs between bases).");
            return;
        }
        if (!session.requiredTeams().contains(c)) {
            Msg.info(player, "This base looks " + c.display + ", which the current gamemode does not use.");
            return;
        }
        session.selected = c;
        Msg.ok(player, "This base is " + c.display + " (selected).");
        refresh();
    }
}
