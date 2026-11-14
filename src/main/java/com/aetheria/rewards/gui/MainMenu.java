package com.aetheria.rewards.gui;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.cosmetics.CosmeticMenu;
import com.aetheria.rewards.reward.Currency;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MainMenu extends Menu {
    public MainMenu(AetheriaRewards plugin, Player p) { super(plugin, p, 5, "Aetheria"); }

    @Override protected void build() {
        set(4, Util.glow(Util.item(Material.NETHER_STAR, "§6§lAetheria §5Recompensas", "§7Coronas en tu inventario: §6" + Currency.count(player))), null);
        set(20, Util.glow(ui(9020, Material.AMETHYST_SHARD, "§d§lCosméticos", "§7Auras, sombreros, mascotas,", "§7alas, efectos y más.", "", "§e▶ Abrir catálogo")),
                e -> new CosmeticMenu(plugin, player).open());
        set(22, Util.glow(ui(9021, Material.GOLDEN_HELMET, "§6§lPase de Batalla", "§7Pista gratuita y premium,", "§7misiones y recompensas.", "", "§e▶ Abrir pase")),
                e -> new com.aetheria.rewards.progression.BattlePassMenu(plugin, player).open());
        set(24, ui(9022, Material.CLOCK, "§b§lMisiones", "§7Diarias, semanales y de temporada.", "", "§e▶ Abrir misiones"),
                e -> new com.aetheria.rewards.progression.QuestMenu(plugin, player, "ALL", () -> new MainMenu(plugin, player).open()).open());
        set(30, ui(9023, Material.SPRUCE_SAPLING, "§a§l❄ Calendario de Adviento", "§7Del 1 al 24 de diciembre.", "", "§e▶ Abrir calendario"),
                e -> new com.aetheria.rewards.progression.AdventMenu(plugin, player).open());
        set(32, ui(9024, Material.PINK_TULIP, "§d§l❤ Evento de Febrero", "§7Amor, amistad y magia.", "", "§e▶ Abrir evento"),
                e -> new com.aetheria.rewards.progression.FebruaryMenu(plugin, player).open());
        close(40);
    }
}
