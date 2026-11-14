package com.aetheria.rewards.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class Util {
    private Util() {}
    public static final String PREFIX = "§5§l✦ §6§lAetheria §8» §7";
    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public static Component c(String legacy) {
        return LEGACY.deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }
    public static String plain(Component comp) { return LEGACY.serialize(comp); }

    public static ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(c(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(c(s));
        if (!l.isEmpty()) meta.lore(l);
        it.setItemMeta(meta);
        return it;
    }
    public static ItemStack item(Material m, int cmd, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(c(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(c(s));
        meta.lore(l);
        if (cmd > 0) meta.setCustomModelData(cmd);
        it.setItemMeta(meta);
        return it;
    }
    /** Relleno invisible (modelo transparente del resource pack) para GUIs con skin. */
    public static ItemStack blank() {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.empty());
        m.setCustomModelData(9000);
        m.setHideTooltip(true);
        it.setItemMeta(m);
        return it;
    }
    public static ItemStack glow(ItemStack it) {
        ItemMeta m = it.getItemMeta();
        m.setEnchantmentGlintOverride(true);
        it.setItemMeta(m);
        return it;
    }
    public static String bar(double frac, int len) {
        int f = (int) Math.round(Math.max(0, Math.min(1, frac)) * len);
        StringBuilder sb = new StringBuilder("§5");
        for (int i = 0; i < len; i++) { if (i == f) sb.append("§8"); sb.append(i < f ? "█" : "▒"); }
        return sb.toString();
    }
    public static String fmtTime(long sec) {
        if (sec < 0) sec = 0;
        long d = sec / 86400, h = (sec % 86400) / 3600, m = (sec % 3600) / 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        return m + "m";
    }
    public static String strip(String s) { return s == null ? "" : s.replaceAll("§.", ""); }
    public static String title(String s) { return "§8§l« §5§l✦ §6§l" + s + " §5§l✦ §8§l»"; }
}
