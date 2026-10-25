package dev.etran.gui;

import dev.etran.BwAutoPlugin;
import dev.etran.model.SetupSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Base class for the chest menus: slot -> click action, re-renderable. */
public abstract class Menu implements InventoryHolder {
    protected final BwAutoPlugin plugin;
    protected final Player player;
    protected final SetupSession session;
    private Inventory inv;
    private boolean closed = true;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(BwAutoPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.session = plugin.sessions().get(player);
    }

    protected abstract String title();

    protected abstract int rows();

    protected abstract void render();

    public void open() {
        inv = Bukkit.createInventory(this, rows() * 9, title());
        closed = false;
        render();
        player.openInventory(inv);
    }

    /** Re-draw in place. Does nothing if the player has since closed or left this menu. */
    public void refresh() {
        if (inv == null || closed) return;
        inv.clear();
        actions.clear();
        render();
    }

    void markClosed() {
        closed = true;
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action);
    }

    protected void fill(Material pane) {
        ItemStack filler = Items.of(pane, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    void handle(InventoryClickEvent e) {
        Consumer<InventoryClickEvent> c = actions.get(e.getRawSlot());
        if (c != null) c.accept(e);
    }

    /** Open another menu one tick later (never swap inventories from inside a click event). */
    protected void go(Menu next) {
        Bukkit.getScheduler().runTask(plugin, next::open);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
