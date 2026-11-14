package com.aetheria.rewards.reward;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Moneda del reino: la "Corona Real" del datapack Aetheria_Moneda (disc_fragment_5, custom_model_data 1001,
 * custom_data {moneda:"corona"}). Para entregarla con el MISMO custom_data que el datapack usamos el comando
 * vanilla /give con la misma cadena de componentes (la API de Paper 1.21.1 no expone custom_data).
 * Para contarla/cobrarla se identifica por material + custom_model_data 1001.
 */
public final class Currency {
    public static final Material MATERIAL = Material.DISC_FRAGMENT_5;
    public static final int CMD = 1001;
    private static final String COMPONENTS =
        "[custom_name='{\"text\":\"Corona Real\",\"color\":\"gold\",\"italic\":false,\"bold\":true}',"
      + "lore=['{\"text\":\"Moneda oficial del reino\",\"color\":\"gray\",\"italic\":false}',"
      + "'{\"text\":\"No se puede usar ni colocar\",\"color\":\"dark_gray\",\"italic\":true}'],"
      + "custom_model_data=1001,custom_data={moneda:\"corona\"},enchantment_glint_override=true]";

    private Currency() {}

    public static boolean isCoin(ItemStack it) {
        if (it == null || it.getType() != MATERIAL || !it.hasItemMeta()) return false;
        ItemMeta m = it.getItemMeta();
        return m.hasCustomModelData() && m.getCustomModelData() == CMD;
    }

    public static ItemStack apiCoin(int amount) {
        ItemStack it = Util.item(MATERIAL, CMD, "§6§lCorona Real", List.of("§7Moneda oficial del reino", "§8§oNo se puede usar ni colocar"));
        Util.glow(it);
        it.setAmount(Math.max(1, Math.min(64, amount)));
        return it;
    }

    public static int count(Player p) {
        int n = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) if (isCoin(it)) n += it.getAmount();
        return n;
    }

    public static boolean take(Player p, int amount) {
        if (count(p) < amount) return false;
        int left = amount;
        ItemStack[] c = p.getInventory().getStorageContents();
        for (int i = 0; i < c.length && left > 0; i++) {
            if (!isCoin(c[i])) continue;
            int t = Math.min(left, c[i].getAmount());
            left -= t;
            if (t == c[i].getAmount()) c[i] = null; else c[i].setAmount(c[i].getAmount() - t);
        }
        p.getInventory().setStorageContents(c);
        return true;
    }

    /** Entrega monedas. Primero via /give (identico al datapack); si falla, via API. */
    public static void give(AetheriaRewards plugin, Player p, int amount) {
        int left = amount;
        while (left > 0) {
            int n = Math.min(64, left);
            left -= n;
            boolean ok = false;
            try {
                ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + p.getUniqueId() + " minecraft:disc_fragment_5" + COMPONENTS + " " + n);
            } catch (Exception ignored) {}
            if (!ok) {
                var rest = p.getInventory().addItem(apiCoin(n));
                for (ItemStack r : rest.values()) {
                    var item = p.getWorld().dropItem(p.getLocation(), r);
                    item.setOwner(p.getUniqueId()); item.setThrower(p.getUniqueId());
                }
            }
        }
    }
}
