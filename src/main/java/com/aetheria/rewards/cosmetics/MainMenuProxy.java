package com.aetheria.rewards.cosmetics;

import com.aetheria.rewards.AetheriaRewards;
import org.bukkit.entity.Player;

/** Atajo para volver al menu principal desde cualquier GUI. */
public class MainMenuProxy {
    private final AetheriaRewards plugin;
    public MainMenuProxy(AetheriaRewards plugin) { this.plugin = plugin; }
    public void open(Player p) { new com.aetheria.rewards.gui.MainMenu(plugin, p).open(); }
}
