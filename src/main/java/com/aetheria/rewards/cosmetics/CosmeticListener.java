package com.aetheria.rewards.cosmetics;

import com.aetheria.rewards.AetheriaRewards;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class CosmeticListener implements Listener {
    private final AetheriaRewards plugin;
    public CosmeticListener(AetheriaRewards plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerJumpEvent e) { plugin.cosmetics().trigger(e.getPlayer(), "JUMP"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) plugin.cosmetics().trigger(p, "DAMAGE");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent e) {
        Player k = e.getEntity().getKiller();
        if (k != null && !(e.getEntity() instanceof Player v && v.equals(k))) plugin.cosmetics().trigger(k, "KILL");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.cosmetics().trigger(p, "SPAWN"), 5L);
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent e) { plugin.cosmetics().refreshActive(e.getPlayer()); }
}
