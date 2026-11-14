package com.aetheria.rewards.reward;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.cosmetics.CosmeticDef;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Unico punto de entrega de recompensas. Siempre en el hilo principal. */
public class RewardService {
    private final AetheriaRewards plugin;
    public RewardService(AetheriaRewards plugin) { this.plugin = plugin; }

    /** Entrega una recompensa. Nunca lanza: ante un fallo la guarda como pendiente para no perderla. */
    public void give(Player p, Reward r) {
        PlayerData d = plugin.data().get(p);
        if (d == null) return;
        plugin.audit().log("REWARD", p, r.type + " " + r.describe(plugin));
        try {
            switch (r.type) {
                case CORONA -> Currency.give(plugin, p, r.amount);
                case XP -> plugin.battlePass().addXp(p, r.amount);
                case KEY -> giveItem(p, plugin.crates().keyItem(r.crateId, r.amount));
                case ITEM -> { if (r.item != null) { ItemStack c = r.item.clone(); c.setAmount(Math.max(1, Math.min(c.getMaxStackSize(), r.amount))); giveItem(p, c); } }
                case COMMAND -> {
                    if (r.command != null) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), r.command.replace("%player%", p.getName()).replace("%uuid%", p.getUniqueId().toString()));
                }
                case COSMETIC -> giveCosmetic(p, d, r.cosmeticId);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Fallo entregando recompensa a " + p.getName() + ": " + ex.getMessage() + " -> guardada como pendiente");
            d.pendingRewards.add(r.serialize());
            plugin.data().markDirty(d);
        }
    }

    private void giveCosmetic(Player p, PlayerData d, String id) {
        CosmeticDef def = plugin.cosmetics().get(id);
        if (def == null) throw new IllegalStateException("cosmetico inexistente " + id);
        giveItem(p, plugin.cosmetics().voucher(def, 1));
        p.sendMessage(Util.c(Util.PREFIX + "Recibiste la hoja de " + def.rarity.color + def.name + "§7. §eClic derecho§7 sobre ella para desbloquearlo."));
        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.3f);
    }

    public void giveItem(Player p, ItemStack it) {
        Map<Integer, ItemStack> rest = p.getInventory().addItem(it);
        for (ItemStack r : rest.values()) {
            Item drop = p.getWorld().dropItem(p.getLocation(), r);
            drop.setOwner(p.getUniqueId());
            drop.setThrower(p.getUniqueId());
        }
        if (!rest.isEmpty()) p.sendMessage(Util.c(Util.PREFIX + "§eTu inventario está lleno: el objeto cayó al suelo (solo tú puedes recogerlo)."));
    }

    /** Reintenta entregar lo pendiente al entrar. */
    public void flushPending(Player p) {
        PlayerData d = plugin.data().get(p);
        if (d == null || d.pendingRewards.isEmpty()) return;
        var copy = new java.util.ArrayList<>(d.pendingRewards);
        d.pendingRewards.clear();
        for (String s : copy) { Reward r = Reward.deserialize(s); if (r != null) give(p, r); }
        plugin.data().markDirty(d);
    }
}
