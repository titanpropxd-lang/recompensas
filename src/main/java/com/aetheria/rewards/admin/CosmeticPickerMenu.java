package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.cosmetics.CosmeticDef;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/** Selector de cosmeticos del catalogo (para pases, adviento, febrero, cofres). */
public class CosmeticPickerMenu extends Menu {
    private final Consumer<CosmeticDef> pick; private final Runnable back;
    private CosmeticDef.Category cat = CosmeticDef.Category.AURAS; private int page = 0;
    private static final int[] SLOTS = new int[28];
    static { int i = 0; for (int r = 1; r <= 4; r++) for (int c = 1; c <= 7; c++) SLOTS[i++] = r * 9 + c; }

    public CosmeticPickerMenu(AetheriaRewards plugin, Player p, Consumer<CosmeticDef> pick, Runnable back) {
        super(plugin, p, 6, "Elegir Cosmético"); this.pick = pick; this.back = back;
    }

    @Override protected void build() {
        CosmeticDef.Category[] cs = CosmeticDef.Category.values();
        for (int i = 0; i < cs.length; i++) {
            CosmeticDef.Category c = cs[i];
            var it = ui(9010 + c.ordinal(), c.icon, (c == cat ? "§6§l" : "§7") + c.label);
            if (c == cat) Util.glow(it);
            set(1 + i, it, e -> { cat = c; page = 0; refresh(); });
        }
        List<CosmeticDef> list = plugin.cosmetics().byCategory(cat);
        int pages = Math.max(1, (list.size() + SLOTS.length - 1) / SLOTS.length);
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = page * SLOTS.length + i;
            if (idx >= list.size()) break;
            CosmeticDef d = list.get(idx);
            set(SLOTS[i], plugin.cosmetics().icon(d, null), e -> pick.accept(d));
        }
        if (page > 0) set(45, ui(9003, Material.ARROW, "§e« Anterior"), e -> { page--; refresh(); }); else back(45, back);
        if (page + 1 < pages) set(53, ui(9004, Material.ARROW, "§eSiguiente »"), e -> { page++; refresh(); }); else close(53);
    }
}
