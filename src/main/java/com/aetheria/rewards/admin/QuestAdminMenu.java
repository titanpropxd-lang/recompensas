package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.progression.QuestManager;
import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class QuestAdminMenu extends Menu {
    public QuestAdminMenu(AetheriaRewards plugin, Player p) { super(plugin, p, 6, "Misiones (Admin)"); }
    private int page = 0;

    @Override protected void build() {
        var list = new java.util.ArrayList<>(plugin.quests().all());
        int per = 28, pages = Math.max(1, (list.size() + per - 1) / per);
        int slot = 10;
        for (int i = page * per; i < Math.min(list.size(), page * per + per); i++) {
            QuestManager.Quest q = list.get(i);
            if (slot % 9 == 8) slot += 2;
            set(slot++, Util.item(Material.PAPER, "§f" + q.name,
                "§7Id: §f" + q.id, "§7Ámbito: §e" + q.scope, "§7Tipo: §e" + q.type, "§7Cantidad: §e" + q.amount, "§7Recompensa: " + q.reward.describe(plugin), "",
                "§e▶ Clic: cambiar recompensa", "§7Shift+clic: cantidad +1 §8(derecho: -1)", "§cQ: eliminar"), e -> {
                    ClickType t = e.getClick();
                    if (t == ClickType.DROP) { plugin.quests().remove(q.id); refresh(); return; }
                    if (t.isShiftClick()) { q.amount = Math.max(1, q.amount + (t.isRightClick() ? -1 : 1)); plugin.quests().save(); refresh(); return; }
                    new RewardEditorMenu(plugin, player, q.reward, r -> { q.reward = r; plugin.quests().save(); }, () -> new QuestAdminMenu(plugin, player).open()).open();
                });
        }
        set(49, Util.glow(Util.item(Material.EMERALD, "§a§l+ Nueva misión", "§7Formato: id;ÁMBITO;TIPO;cantidad;nombre",
            "§8Ámbitos: DAILY WEEKLY SEASON FEBRERO", "§8Tipos: KILL_MOB KILL_PLAYER BREAK_BLOCK FISH OPEN_CRATE FIND_CHEST")), e ->
            plugin.gui().prompt(player, "Escribe: id;ÁMBITO;TIPO;cantidad;nombre", t -> {
                String[] s = t.split(";", 5);
                if (s.length < 5) { player.sendMessage(Util.c("§cFormato inválido.")); return; }
                try {
                    QuestManager.Quest q = new QuestManager.Quest();
                    q.id = s[0].trim().toLowerCase(); q.scope = s[1].trim().toUpperCase(); q.type = s[2].trim().toUpperCase();
                    q.amount = Math.max(1, Integer.parseInt(s[3].trim())); q.name = s[4].trim(); q.desc = "Completa el objetivo."; q.reward = Reward.xp(100);
                    plugin.quests().add(q);
                } catch (NumberFormatException ex) { player.sendMessage(Util.c("§cCantidad inválida.")); return; }
                new QuestAdminMenu(plugin, player).open();
            }));
        if (page > 0) set(45, ui(9003, Material.ARROW, "§e« Anterior"), e -> { page--; refresh(); }); else back(45, () -> new AdminMenu(plugin, player).open());
        if (page + 1 < pages) set(53, ui(9004, Material.ARROW, "§eSiguiente »"), e -> { page++; refresh(); }); else close(53);
    }
}
