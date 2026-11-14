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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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

    /** Clic derecho con una hoja de cosmetico: se consume UNA y se desbloquea (si ya lo tiene, no se gasta). */
    @EventHandler
    public void onVoucher(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        String id = plugin.cosmetics().voucherOf(hand);
        if (id == null) return;
        e.setCancelled(true);
        var def = plugin.cosmetics().get(id);
        var d = plugin.data().get(p);
        if (def == null || d == null) { p.sendMessage(com.aetheria.rewards.util.Util.c(com.aetheria.rewards.util.Util.PREFIX + "§cEsta hoja ya no es válida.")); return; }
        if (d.unlocked.contains(id)) { p.sendMessage(com.aetheria.rewards.util.Util.c(com.aetheria.rewards.util.Util.PREFIX + "§7Ya tienes " + def.rarity.color + def.name + "§7. La hoja no se gastó.")); return; }
        if (hand.getAmount() <= 1) p.getInventory().setItemInMainHand(null); else hand.setAmount(hand.getAmount() - 1);   // se consume primero
        d.unlocked.add(id);
        plugin.data().markDirty(d);
        plugin.audit().log("VOUCHER_REDEEM", p, "cosmetic=" + id);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        p.spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.2);
        p.sendMessage(com.aetheria.rewards.util.Util.c(com.aetheria.rewards.util.Util.PREFIX + "¡Desbloqueaste " + def.rarity.color + def.name + "§7! Equípalo en §e/cosmeticos§7."));
    }
}
