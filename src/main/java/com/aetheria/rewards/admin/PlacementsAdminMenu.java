package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.crate.Placement;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PlacementsAdminMenu extends Menu {
    private final boolean onlyHidden;
    public PlacementsAdminMenu(AetheriaRewards plugin, Player p) { this(plugin, p, false); }
    public PlacementsAdminMenu(AetheriaRewards plugin, Player p, boolean onlyHidden) { super(plugin, p, 6, onlyHidden ? "Cofres Ocultos" : "Posiciones"); this.onlyHidden = onlyHidden; }
    @Override protected void build() {
        int slot = 10;
        for (Placement p : plugin.crates().placements()) {
            if (onlyHidden && p.type != Placement.Type.HIDDEN) continue;
            if (slot >= 44) break;
            if (slot % 9 == 8) slot += 2;
            set(slot++, Util.item(p.type == Placement.Type.HIDDEN ? Material.ENDER_EYE : Material.CHEST, "§f" + p.id,
                "§7Tipo: §e" + p.type, "§7Pool: §f" + p.crateId, "§7Mundo: §f" + p.world, String.format("§7XYZ: §f%.1f %.1f %.1f", p.x, p.y, p.z), "", "§e▶ Clic para gestionar"),
                e -> new PlacementMenu(plugin, player, p).open());
        }
        if (onlyHidden && plugin.crates().placements().stream().noneMatch(x -> x.type == Placement.Type.HIDDEN)) {
            set(22, Util.item(Material.COBWEB, "§7Aún no hay cofres ocultos", "§8Colócalos con /aeadmin hidden <crate>", "§8o desde Crates → tu crate → Colocar cofre oculto."), null);
        }
        back(45, () -> new AdminMenu(plugin, player).open());
        close(53);
    }
}
