package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.crate.Placement;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PlacementsAdminMenu extends Menu {
    public PlacementsAdminMenu(AetheriaRewards plugin, Player p) { super(plugin, p, 6, "Posiciones"); }
    @Override protected void build() {
        int slot = 10;
        for (Placement p : plugin.crates().placements()) {
            if (slot >= 44) break;
            if (slot % 9 == 8) slot += 2;
            set(slot++, Util.item(p.type == Placement.Type.HIDDEN ? Material.ENDER_EYE : Material.CHEST, "§f" + p.id,
                "§7Tipo: §e" + p.type, "§7Mundo: §f" + p.world, String.format("§7XYZ: §f%.1f %.1f %.1f", p.x, p.y, p.z), "", "§e▶ Clic para gestionar"),
                e -> new PlacementMenu(plugin, player, p).open());
        }
        back(45, () -> new AdminMenu(plugin, player).open());
        close(53);
    }
}
