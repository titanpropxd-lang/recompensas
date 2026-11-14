package com.aetheria.rewards.crate;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.model.ModelHandle;
import com.aetheria.rewards.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Cofres ocultos de exploracion: un premio por jugador, brillo tenue al acercarse. */
public class ChestExplorerManager {
    private final AetheriaRewards plugin;

    public ChestExplorerManager(AetheriaRewards plugin) {
        this.plugin = plugin;
        // una sola tarea global: solo compara cofres ocultos (pocos) con jugadores cercanos
        Bukkit.getScheduler().runTaskTimer(plugin, this::shimmer, 100L, 30L);
    }

    private void shimmer() {
        for (Placement p : plugin.crates().placements()) {
            if (p.type != Placement.Type.HIDDEN) continue;
            Location l = p.location();
            if (l == null || !l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) continue;
            for (Player pl : l.getWorld().getPlayers()) {
                if (pl.getLocation().distanceSquared(l) > 64) continue;
                PlayerData d = plugin.data().get(pl);
                if (d != null && d.foundChests.contains(p.id)) continue;
                pl.spawnParticle(Particle.END_ROD, l.clone().add(0, 0.8, 0), 3, 0.35, 0.35, 0.35, 0.01);
            }
        }
    }

    public void discover(Player pl, Placement p, Crate c) {
        PlayerData d = plugin.data().get(pl);
        if (d == null) return;
        if (d.foundChests.contains(p.id)) { pl.sendActionBar(Util.c("§7Ya descubriste este cofre.")); return; }
        if (p.busy) return;
        Crate.Entry e = c.roll();
        if (e == null) return;
        d.foundChests.add(p.id);                // se marca ANTES de entregar: evita duplicados
        plugin.data().markDirty(d);
        p.busy = true;
        Location loc = p.location();
        if (p.handle != null) p.handle.animate("open", ModelHandle.Mode.HOLD);
        if (loc != null) {
            loc.getWorld().playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 0.9f);
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 40, 0.4, 0.5, 0.4, 0.3);
        }
        plugin.rewards().give(pl, e.reward);
        pl.sendMessage(Util.c(Util.PREFIX + "¡Descubriste un cofre oculto! Obtuviste " + e.reward.describe(plugin) + "§7."));
        plugin.quests().progress(pl, "FIND_CHEST", 1);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.handle != null) p.handle.animate("close", ModelHandle.Mode.ONCE);
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.handle != null) p.handle.animate("idle", ModelHandle.Mode.LOOP); p.busy = false; }, 22L);
        }, 60L);
    }
}
