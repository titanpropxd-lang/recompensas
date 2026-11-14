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

/** Calendario navideno medieval: 24 regalos, nieve, luces y acebo. */
public class AdventMenu extends Menu {
    public AdventMenu(AetheriaRewards plugin, Player p) { super(plugin, p, 6, "Adviento"); }

    @Override protected String theme() { return "advent"; }

    @Override protected void frame() {
        if (plugin.skin()) { super.frame(); return; }
        Material[] cyc = {Material.RED_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE};
        for (int i = 0; i < rows * 9; i++) {
            int r = i / 9, c = i % 9;
            boolean edge = r == 0 || r == rows - 1 || c == 0 || c == 8;
            inv.setItem(i, Util.item(edge ? cyc[(r + c) % 4] : Material.BLACK_STAINED_GLASS_PANE, "§r"));
        }
    }

    @Override protected void build() {
        AdventCalendarManager adv = plugin.advent();
        PlayerData d = plugin.data().get(player);
        if (d == null) return;
        set(4, Util.glow(Util.item(Material.SPRUCE_SAPLING, "§c❄ §f§lCalendario de Adviento §a❄", "§7Del 1 al 24 de diciembre de " + adv.year,
                "§7Hoy: " + (adv.today() >= 1 && adv.today() <= 24 ? "§eDía " + adv.today() : adv.today() < 1 ? "§7aún no empieza" : "§7finalizado"),
                "§7Reclamar días pasados: " + (adv.claimPast ? "§así" : "§cno"),
                "§7Reclamados: §a" + d.adventClaimed.size() + "§7/§f24")), null);
        int day = 1;
        for (int r = 1; r <= 4; r++) for (int c = 1; c <= 6; c++, day++) {
            final int dd = day;
            AdventCalendarManager.State st = adv.state(d, dd);
            Reward rw = adv.days.get(dd);
            ItemStack it;
            List<String> lore = new ArrayList<>();
            lore.add(rw == null ? "§8(sin recompensa configurada)" : "§7Recompensa: " + rw.describe(plugin));
            switch (st) {
                case AVAILABLE -> { lore.add(""); lore.add("§e▶ ¡Clic para abrir!"); it = Util.glow(Util.item(dd % 2 == 0 ? Material.CHEST : Material.ENDER_CHEST, "§a§l🎁 Día " + dd, lore.toArray(new String[0]))); }
                case CLAIMED -> { lore.add(""); lore.add("§a✔ Reclamada"); it = Util.item(Material.LIME_DYE, "§aDía " + dd, lore.toArray(new String[0])); }
                case MISSED -> { lore.add(""); lore.add("§cPerdida"); it = Util.item(Material.RED_DYE, "§cDía " + dd, lore.toArray(new String[0])); }
                default -> { lore.set(0, "§8Recompensa oculta"); lore.add(""); lore.add("§7Disponible el §f" + dd + " de diciembre"); it = Util.item(Material.GRAY_DYE, "§7Día " + dd, lore.toArray(new String[0])); }
            }
            it.setAmount(dd);
            set(r * 9 + c, it, e -> { if (adv.claim(player, dd)) refresh(); });
        }
        Material[] deco = {Material.SNOW_BLOCK, Material.SPRUCE_SAPLING, Material.LANTERN, Material.SNOWBALL};
        for (int r = 1; r <= 4; r++) set(r * 9 + 7, Util.item(deco[r - 1], "§f❄"), null);
        back(45, () -> new com.aetheria.rewards.gui.MainMenu(plugin, player).open());
        close(53);
    }
}
