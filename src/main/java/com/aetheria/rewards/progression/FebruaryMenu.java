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
import java.util.Map;

public class FebruaryMenu extends Menu {
    public FebruaryMenu(AetheriaRewards plugin, Player p) { super(plugin, p, 5, "Evento de Febrero"); }

    @Override protected String theme() { return "feb"; }

    @Override protected void frame() {
        if (plugin.skin()) { super.frame(); return; }
        Material[] cyc = {Material.PINK_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE};
        for (int i = 0; i < rows * 9; i++) {
            int r = i / 9, c = i % 9;
            boolean edge = r == 0 || r == rows - 1 || c == 0 || c == 8;
            inv.setItem(i, Util.item(edge ? cyc[(r + c) % 3] : Material.BLACK_STAINED_GLASS_PANE, "§r"));
        }
    }

    @Override protected void build() {
        FebruaryEventManager fe = plugin.february();
        PlayerData d = plugin.data().get(player);
        if (d == null) return;
        int pts = fe.points(d);
        set(4, Util.glow(Util.item(Material.PINK_TULIP, "§d§l❤ Amor, Amistad y Magia ❤", "§7Puntos: §d" + pts + " §7(misiones reclamadas)",
                fe.isActive() ? "§7Termina en: §f" + Util.fmtTime(fe.secondsLeft()) : "§cEvento inactivo")), null);
        int slot = 10;
        for (Map.Entry<Integer, Reward> e : fe.tiers.entrySet()) {
            if (slot >= 26) break;
            if (slot % 9 == 8) slot += 2;
            int need = e.getKey();
            boolean claimed = d.februaryClaimed.contains("tier" + need), ok = pts >= need && fe.isActive();
            ItemStack ic = e.getValue().icon(plugin);
            var m = ic.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = m.hasLore() ? new ArrayList<>(m.lore()) : new ArrayList<>();
            lore.add(Util.c("")); lore.add(Util.c("§7Requiere: §d" + need + " puntos"));
            lore.add(Util.c(claimed ? "§a✔ Reclamada" : ok ? "§e▶ Clic para reclamar" : "§c🔒 Bloqueada"));
            m.lore(lore); ic.setItemMeta(m);
            if (ok && !claimed) Util.glow(ic);
            set(slot++, ic, ev -> { if (fe.claimTier(player, need)) refresh(); });
        }
        set(31, Util.item(Material.WRITABLE_BOOK, "§b§lMisiones del evento", "§7Complétalas para ganar puntos.", "", "§e▶ Abrir"),
                e -> new QuestMenu(plugin, player, "FEBRERO", () -> new FebruaryMenu(plugin, player).open()).open());
        back(36, () -> new com.aetheria.rewards.gui.MainMenu(plugin, player).open());
        close(44);
    }
}
