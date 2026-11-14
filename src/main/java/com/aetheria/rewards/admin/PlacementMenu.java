package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.crate.Placement;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PlacementMenu extends Menu {
    private final Placement p;
    public PlacementMenu(AetheriaRewards plugin, Player pl, Placement p) { super(plugin, pl, 3, p.id); this.p = p; }
    @Override protected void build() {
        set(10, Util.item(Material.ENDER_PEARL, "§bTeletransportarme"), e -> { var l = p.location(); if (l != null) player.teleport(l.clone().add(0, 0.5, 2)); });
        set(12, Util.item(Material.COMPASS, "§eRotar -45°"), e -> { plugin.crates().rotate(p, p.yaw - 45); refresh(); });
        set(13, Util.item(Material.PAPER, "§fOrientación: §e" + Math.round(p.yaw) + "°"), null);
        set(14, Util.item(Material.COMPASS, "§eRotar +45°"), e -> { plugin.crates().rotate(p, p.yaw + 45); refresh(); });
        set(16, Util.item(Material.TNT, "§c✖ Eliminar", "§7Shift+clic para confirmar"), e -> { if (e.isShiftClick()) { plugin.crates().removePlacement(p.id); new PlacementsAdminMenu(plugin, player).open(); } });
        back(18, () -> new PlacementsAdminMenu(plugin, player).open());
        close(26);
    }
}
