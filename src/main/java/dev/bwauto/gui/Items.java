package dev.etran.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Items {
    private Items() {
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static ItemStack of(Material m, String name, String... lore) {
        return of(m, name, Arrays.asList(lore));
    }

    public static ItemStack of(Material m, String name, List<String> lore) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (!lore.isEmpty()) {
                List<String> l = new ArrayList<>();
                for (String s : lore) l.add(color(s));
                meta.setLore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
