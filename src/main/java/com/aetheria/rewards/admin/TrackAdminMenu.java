package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/** Editor de recompensas del pase (BP), calendario (ADVIENTO) y evento (FEBRERO). Clic: editar. Clic derecho: borrar. */
public class TrackAdminMenu extends Menu {
    private final String kind; private int page = 0;
    public TrackAdminMenu(AetheriaRewards plugin, Player p, String kind) { super(plugin, p, 6, "Admin: " + kind); this.kind = kind; }

    private void edit(Reward cur, Consumer<Reward> save) {
        new RewardEditorMenu(plugin, player, cur, r -> { save.accept(r); }, () -> new TrackAdminMenu(plugin, player, kind).open()).open();
    }
    private ItemStack cell(Reward r, String title, String... extra) {
        ItemStack it = r == null ? Util.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, title, "§8(vacío)") : r.icon(plugin);
        var m = it.getItemMeta();
        var lore = new java.util.ArrayList<net.kyori.adventure.text.Component>();
        lore.add(Util.c(title));
        if (r != null) lore.add(Util.c(r.describe(plugin)));
        for (String s : extra) lore.add(Util.c(s));
        lore.add(Util.c("")); lore.add(Util.c("§e▶ Clic: editar  •  §cClic derecho: quitar"));
        m.lore(lore); it.setItemMeta(m);
        return it;
    }

    @Override protected void build() {
        back(45, () -> new AdminMenu(plugin, player).open());
        close(53);
        switch (kind) {
            case "BP" -> buildBp();
            case "ADVIENTO" -> buildAdvent();
            default -> buildFeb();
        }
    }

    private void buildBp() {
        var bp = plugin.battlePass();
        int per = 7, pages = (bp.maxLevel + per - 1) / per;
        set(4, Util.item(Material.NETHER_STAR, "§6" + bp.seasonName, "§7Niveles: " + bp.maxLevel, "§7Edita fechas/XP en battlepass.yml"), null);
        set(9, Util.item(Material.IRON_INGOT, "§7Pista Gratuita"), null);
        set(36, Util.item(Material.AMETHYST_SHARD, "§dPista Premium"), null);
        for (int i = 0; i < per; i++) {
            int lv = page * per + i + 1;
            if (lv > bp.maxLevel) break;
            int col = 1 + i;
            ItemStack n = Util.item(Material.GRAY_STAINED_GLASS_PANE, "§fNivel " + lv); n.setAmount(Math.min(64, lv));
            set(18 + col, n, null);
            set(9 + col, cell(bp.free.get(lv), "§7Gratuita - Nivel " + lv), e -> { if (e.getClick() == ClickType.RIGHT) { bp.free.remove(lv); bp.save(); refresh(); } else edit(bp.free.get(lv), r -> { bp.free.put(lv, r); bp.save(); }); });
            set(27 + col, cell(bp.premium.get(lv), "§dPremium - Nivel " + lv), e -> { if (e.getClick() == ClickType.RIGHT) { bp.premium.remove(lv); bp.save(); refresh(); } else edit(bp.premium.get(lv), r -> { bp.premium.put(lv, r); bp.save(); }); });
        }
        if (page > 0) set(46, ui(9003, Material.ARROW, "§e« Anterior"), e -> { page--; refresh(); });
        if (page + 1 < pages) set(52, ui(9004, Material.ARROW, "§eSiguiente »"), e -> { page++; refresh(); });
    }

    private void buildAdvent() {
        var adv = plugin.advent();
        set(4, Util.item(Material.SPRUCE_SAPLING, "§aCalendario " + adv.year, "§7Define la recompensa de cada día."), null);
        set(49, Util.item(adv.claimPast ? Material.LIME_DYE : Material.RED_DYE, "§fReclamar días pasados: " + (adv.claimPast ? "§aSÍ" : "§cNO"), "§7Clic para alternar"), e -> { adv.claimPast = !adv.claimPast; adv.save(); refresh(); });
        int day = 1;
        for (int r = 1; r <= 4; r++) for (int c = 1; c <= 6; c++, day++) {
            final int dd = day;
            ItemStack it = cell(adv.days.get(dd), "§a§lDía " + dd);
            it.setAmount(dd);
            set(r * 9 + c, it, e -> { if (e.getClick() == ClickType.RIGHT) { adv.days.remove(dd); adv.save(); refresh(); } else edit(adv.days.get(dd), rw -> { adv.days.put(dd, rw); adv.save(); }); });
        }
    }

    private void buildFeb() {
        var fe = plugin.february();
        set(4, Util.item(Material.PINK_TULIP, "§dEvento de Febrero", "§7Tramos por puntos (misiones reclamadas)."), null);
        int slot = 10;
        for (Map.Entry<Integer, Reward> en : fe.tiers.entrySet()) {
            if (slot >= 35) break;
            if (slot % 9 == 8) slot += 2;
            int pts = en.getKey();
            set(slot++, cell(en.getValue(), "§d" + pts + " puntos"), e -> { if (e.getClick() == ClickType.RIGHT) { fe.tiers.remove(pts); fe.save(); refresh(); } else edit(fe.tiers.get(pts), r -> { fe.tiers.put(pts, r); fe.save(); }); });
        }
        set(49, Util.glow(Util.item(Material.EMERALD, "§a§l+ Añadir tramo")), e ->
            plugin.gui().prompt(player, "Puntos requeridos (número):", t -> {
                try { int p = Integer.parseInt(t.trim()); edit(null, r -> { fe.tiers.put(p, r); fe.save(); }); } catch (NumberFormatException ex) { player.sendMessage("§cNúmero inválido."); }
            }));
    }
}
