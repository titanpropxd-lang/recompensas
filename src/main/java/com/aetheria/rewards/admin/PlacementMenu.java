package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.crate.Crate;
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
        Crate crate = plugin.crates().get(p.crateId);
        long uses = plugin.crates().placements().stream().filter(x -> p.crateId.equals(x.crateId)).count();
        if (crate != null) {
            set(20, Util.glow(Util.item(Material.CHEST, "§e§l📦 Editar recompensas", "§7Pool: §f" + crate.name.replaceAll("§.", ""), "§7Recompensas: §f" + crate.rewards.size(),
                    "§7Cofres que usan este pool: §f" + uses, "", "§e▶ Clic para editar objetos y probabilidades")),
                    e -> new CrateRewardsMenu(plugin, player, crate).open());
            set(22, Util.item(Material.WRITABLE_BOOK, "§b§l✎ Pool propio para ESTE cofre", "§7Copia las recompensas actuales a una lista", "§7solo de este cofre, para cambiarlas", "§7sin afectar a los demás cofres.", "", "§e▶ Clic para crear y editar"), e -> {
                String id = ("cofre_" + p.id).toLowerCase().replaceAll("[^a-z0-9_]", "");
                if (plugin.crates().get(id) != null) { player.sendMessage(Util.c(Util.PREFIX + "§7Este cofre ya tiene pool propio.")); new CrateRewardsMenu(plugin, player, plugin.crates().get(id)).open(); return; }
                Crate n = plugin.crates().createCrate(id, "Cofre " + p.id);
                n.name = crate.name; n.rarity = crate.rarity; n.model = crate.model; n.scale = crate.scale; n.fallbackBlock = crate.fallbackBlock;
                n.keyName = crate.keyName; n.keyCmd = crate.keyCmd; n.keyMaterial = crate.keyMaterial;
                for (Crate.Entry en : crate.rewards) n.rewards.add(new Crate.Entry(en.reward.copy(), en.weight, en.rarity));
                plugin.crates().saveCrates();
                p.crateId = id;
                plugin.crates().rotate(p, p.yaw);     // guarda y recrea lo visual
                player.sendMessage(Util.c(Util.PREFIX + "§aPool propio creado: §f" + id));
                new CrateRewardsMenu(plugin, player, n).open();
            });
        }
        back(18, () -> new PlacementsAdminMenu(plugin, player).open());
        close(26);
    }
}
