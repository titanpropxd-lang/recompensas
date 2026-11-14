package com.aetheria.rewards.progression;

import com.aetheria.rewards.AetheriaRewards;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

public class QuestListener implements Listener {
    private final AetheriaRewards plugin;
    public QuestListener(AetheriaRewards plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent e) {
        Player k = e.getEntity().getKiller();
        if (k == null) return;
        if (e.getEntity() instanceof Player) plugin.quests().progress(k, "KILL_PLAYER", 1);
        else if (e.getEntity() instanceof Monster) plugin.quests().progress(k, "KILL_MOB", 1);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) { plugin.quests().progress(e.getPlayer(), "BREAK_BLOCK", 1); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) plugin.quests().progress(e.getPlayer(), "FISH", 1);
    }
}
