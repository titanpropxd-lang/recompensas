package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Panel de administracion: todo se gestiona desde aqui. */
public class AdminMenu extends Menu {
    public AdminMenu(AetheriaRewards plugin, Player p) { super(plugin, p, 5, "Administración"); }

    @Override protected void build() {
        set(4, Util.glow(Util.item(Material.NETHER_STAR, "§6§lPanel de Administración", "§7Aetheria Rewards")), null);
        set(19, Util.item(Material.ENDER_CHEST, "§d§lCrates y Llaves", "§7Recompensas, probabilidades,", "§7modelos y llaves."), e -> new CrateAdminMenu(plugin, player).open());
        set(20, Util.item(Material.AMETHYST_SHARD, "§d§lCosméticos", "§7Ajustar posición, rotación,", "§7escala y publicar."), e -> new CosmeticAdminMenu(plugin, player).open());
        set(21, Util.item(Material.GOLDEN_HELMET, "§6§lPase de Batalla", "§7Niveles y recompensas."), e -> new TrackAdminMenu(plugin, player, "BP").open());
        set(22, Util.item(Material.CLOCK, "§b§lMisiones", "§7Diarias, semanales, temporada, evento."), e -> new QuestAdminMenu(plugin, player).open());
        set(23, Util.item(Material.SPRUCE_SAPLING, "§a§lAdviento", "§724 casillas."), e -> new TrackAdminMenu(plugin, player, "ADVIENTO").open());
        set(24, Util.item(Material.PINK_TULIP, "§d§lEvento de Febrero", "§7Tramos de recompensas."), e -> new TrackAdminMenu(plugin, player, "FEBRERO").open());
        set(25, Util.item(Material.COMPASS, "§e§lPosiciones", "§7Cofres colocados en el mundo."), e -> new PlacementsAdminMenu(plugin, player).open());
        set(31, Util.item(Material.REPEATING_COMMAND_BLOCK, "§a§lRecargar configuración", "§7Relee YAML sin reiniciar."), e -> { plugin.reloadAll(); player.sendMessage(Util.c(Util.PREFIX + "§aConfiguración recargada.")); });
        close(40);
    }
}
