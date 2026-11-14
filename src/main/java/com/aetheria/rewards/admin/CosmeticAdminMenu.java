package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.cosmetics.CosmeticDef;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Lista de cosmeticos para ajustar. Los no publicados solo los ven admins (para probarlos antes de lanzar). */
public class CosmeticAdminMenu extends Menu {
    public CosmeticAdminMenu(AetheriaRewards plugin, Player p) { super(plugin, p, 6, "Cosméticos (Admin)"); }
    private CosmeticDef.Category cat = CosmeticDef.Category.AURAS; private int page = 0;
    private static final int[] SLOTS = new int[28];
    static { int i = 0; for (int r = 1; r <= 4; r++) for (int c = 1; c <= 7; c++) SLOTS[i++] = r * 9 + c; }

    @Override protected void build() {
        CosmeticDef.Category[] cs = CosmeticDef.Category.values();
        for (int i = 0; i < cs.length; i++) {
            CosmeticDef.Category c = cs[i];
            var it = ui(9010 + c.ordinal(), c.icon, (c == cat ? "§6§l" : "§7") + c.label);
            if (c == cat) Util.glow(it);
            set(1 + i, it, e -> { cat = c; page = 0; refresh(); });
        }
        var list = plugin.cosmetics().byCategory(cat);
        int pages = Math.max(1, (list.size() + SLOTS.length - 1) / SLOTS.length);
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = page * SLOTS.length + i;
            if (idx >= list.size()) break;
            CosmeticDef d = list.get(idx);
            var ic = plugin.cosmetics().icon(d, null);
            var m = ic.getItemMeta();
            var l = new java.util.ArrayList<>(m.lore());
            l.add(Util.c("")); l.add(Util.c(d.published ? "§aPublicado" : "§cNO publicado (solo admins)")); l.add(Util.c("§e▶ Clic para ajustar"));
            m.lore(l); ic.setItemMeta(m);
            set(SLOTS[i], ic, e -> new CosmeticTuneMenu(plugin, player, d).open());
        }
        if (page > 0) set(45, ui(9003, Material.ARROW, "§e« Anterior"), e -> { page--; refresh(); }); else back(45, () -> new AdminMenu(plugin, player).open());
        if (page + 1 < pages) set(53, ui(9004, Material.ARROW, "§eSiguiente »"), e -> { page++; refresh(); }); else close(53);
    }
}

/** Ajuste fino: posicion, rotacion, escala; probar sobre uno mismo; publicar. */
class CosmeticTuneMenu extends Menu {
    private final CosmeticDef d;
    CosmeticTuneMenu(AetheriaRewards plugin, Player p, CosmeticDef d) { super(plugin, p, 5, "Ajuste: " + d.name.replaceAll("§.", "")); this.d = d; }

    private void adj(int slot, String label, java.util.function.DoubleConsumer apply, double step, double cur) {
        set(slot - 1, Util.item(Material.RED_STAINED_GLASS_PANE, "§c- " + label, "§7Clic: -" + step + "  •  Shift: -" + step * 5), e -> { apply.accept(-step * (e.isShiftClick() ? 5 : 1)); retry(); });
        set(slot, Util.item(Material.PAPER, "§f" + label + ": §e" + String.format("%.3f", cur)), null);
        set(slot + 1, Util.item(Material.LIME_STAINED_GLASS_PANE, "§a+ " + label, "§7Clic: +" + step + "  •  Shift: +" + step * 5), e -> { apply.accept(step * (e.isShiftClick() ? 5 : 1)); retry(); });
    }
    private void retry() { plugin.cosmetics().preview(player, d); refresh(); }

    @Override protected void build() {
        set(4, plugin.cosmetics().icon(d, null), null);
        adj(11, "X (derecha)", v -> d.ox += v, 0.05, d.ox);
        adj(14, "Y (altura)", v -> d.oy += v, 0.05, d.oy);
        adj(20, "Z (atrás)", v -> d.oz += v, 0.05, d.oz);
        adj(23, "Rotación", v -> d.yaw += v * 100, 0.15, d.yaw);
        adj(29, "Escala", v -> d.scale = Math.max(0.1, d.scale + v), 0.1, d.scale);
        set(31, Util.item(Material.ENDER_EYE, "§b👁 Probar en mí", "§7Previsualiza " + com.aetheria.rewards.cosmetics.CosmeticManager.PREVIEW_SECONDS + "s sin desbloquear."), e -> plugin.cosmetics().preview(player, d));
        set(32, Util.item(d.published ? Material.LIME_DYE : Material.RED_DYE, d.published ? "§aPublicado" : "§cNo publicado", "§7Clic para alternar"), e -> { d.published = !d.published; refresh(); });
        set(33, Util.item(Material.CHEST, "§e🎁 Desbloquearme este cosmético"), e -> plugin.rewards().give(player, Reward.cosmetic(d.id)));
        set(40, Util.glow(Util.item(Material.EMERALD, "§a§l✔ Guardar en catalog.yml")), e -> { plugin.cosmetics().saveTuning(d); player.sendMessage(Util.c(Util.PREFIX + "§aGuardado.")); });
        back(36, () -> new CosmeticAdminMenu(plugin, player).open());
        close(44);
    }
}
