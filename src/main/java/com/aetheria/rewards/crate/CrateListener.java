package com.aetheria.rewards.crate;

import com.aetheria.rewards.AetheriaRewards;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

public class CrateListener implements Listener {
    private final AetheriaRewards plugin;
    public CrateListener(AetheriaRewards plugin) { this.plugin = plugin; }

    private Placement of(Entity e) {
        if (!(e instanceof Interaction)) return null;
        String id = e.getPersistentDataContainer().get(plugin.crates().placementTag, PersistentDataType.STRING);
        return id == null ? null : plugin.crates().placement(id);
    }

    @EventHandler
    public void onRight(PlayerInteractEntityEvent e) {
        if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) { if (of(e.getRightClicked()) != null) e.setCancelled(true); return; }
        Placement p = of(e.getRightClicked());
        if (p == null) return;
        e.setCancelled(true);
        plugin.crates().interact(e.getPlayer(), p, e.getPlayer().isSneaking());
    }

    @EventHandler
    public void onLeft(EntityDamageByEntityEvent e) {
        Placement p = of(e.getEntity());
        if (p == null) return;
        e.setCancelled(true);
        if (e.getDamager() instanceof Player pl) {
            if (pl.hasPermission("rewards.admin") && pl.isSneaking() && pl.getInventory().getItemInMainHand().getType() == org.bukkit.Material.BLAZE_ROD) {
                new com.aetheria.rewards.admin.PlacementMenu(plugin, pl, p).open(); return;
            }
            Crate c = plugin.crates().get(p.crateId);
            if (c != null && p.type == Placement.Type.CRATE) new CratePreviewMenu(plugin, pl, c, null).open();
        }
    }

    @EventHandler
    public void onAttack(io.papermc.paper.event.player.PrePlayerAttackEntityEvent e) {
        Placement p = of(e.getAttacked());
        if (p == null) return;
        e.setCancelled(true);
        Crate c = plugin.crates().get(p.crateId);
        if (c != null && p.type == Placement.Type.CRATE) new CratePreviewMenu(plugin, e.getPlayer(), c, null).open();
    }

    @EventHandler
    public void onChunk(ChunkLoadEvent e) { plugin.crates().onChunkLoad(e.getChunk()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { plugin.crates().finishSession(e.getPlayer()); }
}
