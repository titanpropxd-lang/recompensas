package com.aetheria.rewards.progression;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BattlePassMenu extends Menu {
    private int page = 0;
    private static final int PER_PAGE = 7;
    public BattlePassMenu(AetheriaRewards plugin, Player p) { super(plugin, p, 6, "Pase de Batalla"); }

    @Override protected void build() {
        BattlePassManager bp = plugin.battlePass();
        PlayerData d = plugin.data().get(player);
        if (d == null) return;
        int lv = bp.level(d);
        long into = bp.xpIntoLevel(d);
        int need = lv >= bp.maxLevel ? 0 : bp.xpForLevel(lv + 1);
        boolean prem = bp.hasPremium(player);

        set(2, Util.item(prem ? Material.GOLDEN_APPLE : Material.IRON_INGOT, prem ? "§6§l✦ Pase Premium activo" : "§7Pase Gratuito",
                prem ? "§7Tienes acceso a ambas pistas." : "§7La pista premium requiere el permiso", "§e" + bp.premiumPermission), null);
        set(4, Util.glow(Util.item(Material.NETHER_STAR, "§6§l" + bp.seasonName, "§7Nivel: §e" + lv + "§7/§f" + bp.maxLevel,
                lv >= bp.maxLevel ? "§a¡Nivel máximo!" : "§7XP: §b" + into + "§7/§b" + need + " " + Util.bar(need == 0 ? 1 : (double) into / need, 12),
                "§7Tiempo restante: §f" + (bp.isActive() ? Util.fmtTime(bp.secondsLeft()) : "§cinactiva"))), null);
        set(6, Util.item(Material.CLOCK, "§b§lMisiones", "§7Gana XP completando misiones.", "", "§e▶ Abrir"),
                e -> new QuestMenu(plugin, player, "DAILY", () -> new BattlePassMenu(plugin, player).open()).open());

        int pages = (bp.maxLevel + PER_PAGE - 1) / PER_PAGE;
        if (page >= pages) page = pages - 1;
        set(9, Util.item(Material.IRON_INGOT, "§7Pista Gratuita"), null);
        set(36 + 0, Util.item(Material.AMETHYST_SHARD, "§dPista Premium"), null);
        for (int i = 0; i < PER_PAGE; i++) {
            int level = page * PER_PAGE + i + 1;
            if (level > bp.maxLevel) break;
            int col = 1 + i;
            boolean reached = level <= lv;
            ItemStack lvl = Util.item(reached ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
                    (reached ? "§a" : "§7") + "Nivel " + level, reached ? "§aAlcanzado" : "§8Faltan " + (level - lv) + " nivel(es)");
            lvl.setAmount(Math.min(64, level));
            set(18 + col, lvl, null);
            track(18 - 9 + col + 0, level, false, d, reached, prem);
            track(27 + col, level, true, d, reached, prem);
        }
        // fila inferior
        if (page > 0) set(45, ui(9003, Material.ARROW, "§e« Niveles anteriores"), e -> { page--; refresh(); });
        else back(45, () -> new com.aetheria.rewards.gui.MainMenu(plugin, player).open());
        set(49, Util.glow(ui(9007, Material.CHEST, "§6§lReclamar todo", "§7Reclama todo lo disponible", "§7en ambas pistas.")), e -> {
            int n = bp.claimAll(player);
            player.sendMessage(Util.c(Util.PREFIX + (n > 0 ? "Reclamaste §e" + n + " §7recompensa(s)." : "§7No hay recompensas disponibles.")));
            refresh();
        });
        if ((page + 1) < pages) set(53, ui(9004, Material.ARROW, "§eNiveles siguientes »"), e -> { page++; refresh(); }); else close(53);
    }

    private void track(int slot, int level, boolean premiumTrack, PlayerData d, boolean reached, boolean hasPrem) {
        BattlePassManager bp = plugin.battlePass();
        Reward r = (premiumTrack ? bp.premium : bp.free).get(level);
        if (r == null) { set(slot, Util.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§8— sin recompensa —"), null); return; }
        boolean claimed = (premiumTrack ? d.bpClaimedPremium : d.bpClaimedFree).contains(level);
        ItemStack ic = r.icon(plugin);
        var m = ic.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = m.hasLore() ? new ArrayList<>(m.lore()) : new ArrayList<>();
        lore.add(Util.c("")); lore.add(Util.c(r.describe(plugin)));
        lore.add(Util.c(premiumTrack ? "§d✦ Pista Premium" : "§7Pista Gratuita"));
        if (claimed) lore.add(Util.c("§a✔ Reclamada"));
        else if (premiumTrack && !hasPrem) lore.add(Util.c("§c🔒 Requiere pase premium"));
        else if (!reached) lore.add(Util.c("§c🔒 Alcanza el nivel " + level));
        else lore.add(Util.c("§e▶ Clic para reclamar"));
        m.lore(lore);
        ic.setItemMeta(m);
        if (!claimed && reached && (!premiumTrack || hasPrem)) Util.glow(ic);
        set(slot, ic, e -> { if (bp.claim(player, level, premiumTrack)) refresh(); });
    }
}
