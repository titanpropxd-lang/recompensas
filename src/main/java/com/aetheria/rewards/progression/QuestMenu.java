package com.aetheria.rewards.progression;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class QuestMenu extends Menu {
    private String scope; private final Runnable back;
    private static final String[] SCOPES = {"DAILY", "WEEKLY", "SEASON", "FEBRERO"};
    private static final String[] NAMES = {"Diarias", "Semanales", "De temporada", "Febrero ❤"};
    private static final Material[] ICONS = {Material.SUNFLOWER, Material.CLOCK, Material.NETHER_STAR, Material.PINK_TULIP};
    public QuestMenu(AetheriaRewards plugin, Player p, String scope, Runnable back) {
        super(plugin, p, 5, "Misiones"); this.scope = scope.equals("ALL") ? "DAILY" : scope; this.back = back;
    }

    @Override protected void build() {
        PlayerData d = plugin.data().get(player);
        if (d == null) return;
        plugin.quests().checkResets(d);
        for (int i = 0; i < SCOPES.length; i++) {
            String sc = SCOPES[i];
            var it = Util.item(ICONS[i], (sc.equals(scope) ? "§6§l" : "§7") + NAMES[i], sc.equals(scope) ? "§a▶ Seleccionado" : "§eClic para ver");
            if (sc.equals(scope)) Util.glow(it);
            set(2 + i * 1 + (i >= 2 ? 1 : 0), it, e -> { scope = sc; refresh(); });
        }
        String reset = scope.equals("DAILY") ? "Reinicio en §f" + Util.fmtTime(plugin.quests().secondsToDailyReset())
                : scope.equals("WEEKLY") ? "Reinicio en §f" + Util.fmtTime(plugin.quests().secondsToWeeklyReset())
                : scope.equals("SEASON") ? "Termina en §f" + Util.fmtTime(plugin.battlePass().secondsLeft())
                : plugin.february().isActive() ? "Termina en §f" + Util.fmtTime(plugin.february().secondsLeft()) : "§cEvento inactivo";
        set(4 + 0 * 0 + 0, Util.item(Material.BOOK, "§e" + reset), null);
        List<QuestManager.Quest> list = plugin.quests().scope(scope);
        int slot = 10;
        for (QuestManager.Quest q : list) {
            if (slot >= 35) break;
            if (slot % 9 == 8) slot += 2;
            int cur = plugin.quests().progressOf(d, q);
            boolean claimed = d.questsClaimed.contains(q.id), done = cur >= q.amount;
            ItemStack it = Util.item(claimed ? Material.LIME_DYE : done ? Material.GOLD_INGOT : Material.PAPER,
                    (claimed ? "§a" : done ? "§6§l" : "§f") + q.name,
                    "§7" + q.desc, "", "§7Progreso: §e" + cur + "§7/§e" + q.amount, Util.bar((double) cur / q.amount, 14), "",
                    "§7Recompensa: " + q.reward.describe(plugin), claimed ? "§a✔ Reclamada" : done ? "§e▶ Clic para reclamar" : "§8En progreso");
            if (done && !claimed) Util.glow(it);
            set(slot++, it, e -> { if (plugin.quests().claim(player, q)) refresh(); });
        }
        back(36, back);
        close(44);
    }
}
