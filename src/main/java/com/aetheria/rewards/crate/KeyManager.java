package com.aetheria.rewards.crate;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Llaves: items con modelo propio (resource pack) y etiqueta PDC que las vincula a su crate. */
public class KeyManager {
    private final AetheriaRewards plugin;
    public final NamespacedKey keyTag;
    public KeyManager(AetheriaRewards plugin) { this.plugin = plugin; this.keyTag = new NamespacedKey(plugin, "crate_key"); }

    public ItemStack create(Crate c, int amount) {
        ItemStack it = Util.item(c.keyMaterial, c.keyCmd, c.rarity.color + "§l" + c.keyName,
            List.of(c.rarity.tag() + " §8• §7Llave", "", "§7Abre: " + c.name, "§8Haz clic derecho sobre el cofre", "§8con la llave en tu inventario."));
        ItemMeta m = it.getItemMeta();
        m.getPersistentDataContainer().set(keyTag, PersistentDataType.STRING, c.id);
        m.setEnchantmentGlintOverride(true);
        it.setItemMeta(m);
        it.setAmount(Math.max(1, Math.min(64, amount)));
        return it;
    }

    public String crateOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(keyTag, PersistentDataType.STRING);
    }

    public int count(Player p, String crateId) {
        int n = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) if (crateId.equals(crateOf(it))) n += it.getAmount();
        return n;
    }

    /** Consume UNA llave de forma atomica (hilo principal). Devuelve false si no habia. */
    public boolean consume(Player p, String crateId) {
        ItemStack[] c = p.getInventory().getStorageContents();
        for (int i = 0; i < c.length; i++) {
            if (c[i] != null && crateId.equals(crateOf(c[i]))) {
                if (c[i].getAmount() <= 1) c[i] = null; else c[i].setAmount(c[i].getAmount() - 1);
                p.getInventory().setStorageContents(c);
                return true;
            }
        }
        return false;
    }
}
