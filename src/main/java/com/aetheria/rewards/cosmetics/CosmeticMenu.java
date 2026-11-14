package com.aetheria.rewards.cosmetics;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Catalogo del jugador: categorias, favoritos, desbloqueados/bloqueados, buscador, previsualizar, equipar. */
public class CosmeticMenu extends Menu {
    private static final int[] TABS = {1, 2, 3, 4, 5, 6, 7};
    private CosmeticDef.Category cat = CosmeticDef.Category.AURAS;
    private boolean favorites = false;
    private int filter = 0;       // 0 todos, 1 desbloqueados, 2 bloqueados
    private String search = "";
    private int page = 0;
    private static final int[] SLOTS = new int[28];
    static { int i = 0; for (int r = 1; r <= 4; r++) for (int c = 1; c <= 7; c++) SLOTS[i++] = r * 9 + c; }

    public CosmeticMenu(AetheriaRewards plugin, Player p) { super(plugin, p, 6, "Cosméticos"); }

    @Override protected void build() {
        PlayerData d = plugin.data().get(player);
        CosmeticDef.Category[] cs = CosmeticDef.Category.values();
        for (int i = 0; i < cs.length; i++) {
            CosmeticDef.Category c = cs[i];
            boolean sel = !favorites && c == cat;
            var it = ui(9010 + c.ordinal(), c.icon, (sel ? "§6§l" : "§7") + c.label, sel ? "§a▶ Seleccionado" : "§eClic para ver");
            if (sel) Util.glow(it);
            set(TABS[i], it, e -> { cat = c; favorites = false; page = 0; refresh(); });
        }
        var fav = ui(9008, Material.NETHER_STAR, (favorites ? "§6§l" : "§7") + "★ Favoritos", "§7" + (d == null ? 0 : d.favorites.size()) + " marcados");
        if (favorites) Util.glow(fav);
        set(TABS[6], fav, e -> { favorites = true; page = 0; refresh(); });

        List<CosmeticDef> list = new ArrayList<>();
        for (CosmeticDef def : plugin.cosmetics().all()) {
            if (!def.published && !player.hasPermission("rewards.admin")) continue;
            if (favorites ? (d == null || !d.favorites.contains(def.id)) : def.category != cat) continue;
            if (d == null || !plugin.cosmetics().owned(d, def)) continue;
            if (!search.isEmpty() && !def.name.toLowerCase().contains(search.toLowerCase())) continue;
            list.add(def);
        }
        list.sort(Comparator.comparing((CosmeticDef x) -> x.rarity).thenComparing(x -> x.name));
        int pages = Math.max(1, (list.size() + SLOTS.length - 1) / SLOTS.length);
        if (page >= pages) page = pages - 1;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = page * SLOTS.length + i;
            if (idx >= list.size()) break;
            CosmeticDef def = list.get(idx);
            set(SLOTS[i], plugin.cosmetics().icon(def, d), e -> click(def, e.getClick()));
        }
        if (list.isEmpty()) set(22, Util.item(Material.COBWEB, "§7No tienes cosméticos aquí", "§8Consigue hojas de cosmético y haz clic derecho", "§8sobre ellas para desbloquearlos."));

        // barra inferior
        if (page > 0) set(45, ui(9003, Material.ARROW, "§e« Página anterior"), e -> { page--; refresh(); });
        else back(45, () -> new MainMenuProxy(plugin).open(player));
        set(47, ui(9006, Material.HOPPER, "§7Solo ves lo que posees", "§8Consigue más con hojas de cosmético", "§8(cofres, pase, adviento y eventos)."), null);
        set(48, ui(9005, Material.COMPASS, "§bBuscar: §f" + (search.isEmpty() ? "—" : search), "§7Clic: buscar por nombre", "§7Clic derecho: limpiar"), e -> {
            if (e.getClick() == ClickType.RIGHT) { search = ""; refresh(); }
            else plugin.gui().prompt(player, "Escribe parte del nombre del cosmético:", t -> { search = t; page = 0; open(); });
        });
        long own = d == null ? 0 : plugin.cosmetics().all().stream().filter(x -> plugin.cosmetics().owned(d, x)).count();
        set(49, Util.item(Material.BOOK, "§6Colección", "§7Desbloqueados: §a" + own + "§7/§f" + plugin.cosmetics().all().size(),
                "§7Página §f" + (page + 1) + "§7/§f" + pages), null);
        set(50, Util.item(Material.LAVA_BUCKET, "§cQuitar todo", "§7Desequipa todos tus cosméticos"), e -> { plugin.cosmetics().unequipAll(player); refresh(); });
        if (page + 1 < pages) set(53, ui(9004, Material.ARROW, "§ePágina siguiente »"), e -> { page++; refresh(); });
        else close(53);
    }

    private void click(CosmeticDef def, ClickType t) {
        PlayerData d = plugin.data().get(player);
        if (d == null) return;
        if (t.isShiftClick()) { plugin.cosmetics().preview(player, def); return; }
        if (t.isRightClick()) {
            if (!d.favorites.remove(def.id)) d.favorites.add(def.id);
            plugin.data().markDirty(d); refresh(); return;
        }
        if (!plugin.cosmetics().owned(d, def)) {
            player.sendMessage(Util.c(Util.PREFIX + "§cBloqueado. §7Usa §eShift+clic §7para previsualizarlo."));
            return;
        }
        if (def.id.equals(d.equipped.get(def.slot))) plugin.cosmetics().unequip(player, def.slot);
        else plugin.cosmetics().equip(player, def);
        refresh();
    }
}
