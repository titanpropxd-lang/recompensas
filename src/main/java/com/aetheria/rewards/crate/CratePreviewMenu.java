package com.aetheria.rewards.crate;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Lista de recompensas y probabilidades de un cofre. Con 'onBack' funciona dentro del flujo de administracion. */
public class CratePreviewMenu extends Menu {
    private final Crate crate; private final Runnable onBack; private int page = 0;
    private static final int[] SLOTS = new int[28];
    static { int i = 0; for (int r = 1; r <= 4; r++) for (int c = 1; c <= 7; c++) SLOTS[i++] = r * 9 + c; }

    public CratePreviewMenu(AetheriaRewards plugin, Player p, Crate c, Runnable onBack) {
        super(plugin, p, 6, c.name.replaceAll("§.", "") + " - Recompensas");
        this.crate = c; this.onBack = onBack;
    }

    @Override protected void build() {
        List<Crate.Entry> list = new ArrayList<>(crate.rewards);
        list.sort((a, b) -> Double.compare(b.weight, a.weight));
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = page * SLOTS.length + i;
            if (idx >= list.size()) break;
            Crate.Entry e = list.get(idx);
            ItemStack ic = e.reward.icon(plugin);
            var m = ic.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = m.hasLore() ? new ArrayList<>(m.lore()) : new ArrayList<>();
            lore.add(Util.c("")); lore.add(Util.c(e.rarity.tag()));
            lore.add(Util.c("§7Probabilidad: §e" + String.format("%.2f", crate.chance(e)) + "%"));
            m.lore(lore); ic.setItemMeta(m);
            set(SLOTS[i], ic);
        }
        int pages = Math.max(1, (list.size() + SLOTS.length - 1) / SLOTS.length);
        java.util.EnumMap<com.aetheria.rewards.util.Rarity, Double> byRar = new java.util.EnumMap<>(com.aetheria.rewards.util.Rarity.class);
        for (Crate.Entry en : crate.rewards) byRar.merge(en.rarity, crate.chance(en), Double::sum);
        java.util.List<String> rl = new java.util.ArrayList<>();
        rl.add("§7Reparto de probabilidades:");
        for (var r : com.aetheria.rewards.util.Rarity.values()) if (byRar.containsKey(r)) rl.add(r.color + r.label + " §8» §f" + String.format("%.1f", byRar.get(r)) + "%");
        set(47, Util.glow(Util.item(Material.AMETHYST_CLUSTER, "§6§lProbabilidad por rareza", rl.toArray(new String[0]))), null);
        if (page > 0) set(45, ui(9003, Material.ARROW, "§e« Anterior"), e -> { page--; refresh(); });
        else if (onBack != null) back(45, onBack); 
        set(49, Util.item(Material.BOOK, "§6" + crate.name.replaceAll("§.", ""), "§7Recompensas: §f" + crate.rewards.size(), "§7Página " + (page + 1) + "/" + pages), null);
        if (page + 1 < pages) set(53, ui(9004, Material.ARROW, "§eSiguiente »"), e -> { page++; refresh(); }); else close(53);
    }
}
