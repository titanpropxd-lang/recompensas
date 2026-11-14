package com.aetheria.rewards.reward;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.cosmetics.CosmeticDef;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Una recompensa configurable. Se guarda en YAML y se edita desde las GUIs. */
public class Reward {
    public enum Type { ITEM, CORONA, COSMETIC, KEY, COMMAND, XP }

    public Type type = Type.CORONA;
    public int amount = 1;
    public ItemStack item;          // ITEM
    public String cosmeticId;       // COSMETIC
    public String crateId;          // KEY
    public String command;          // COMMAND  (%player% = nombre)
    public String display;          // texto para COMMAND

    public static Reward corona(int n) { Reward r = new Reward(); r.type = Type.CORONA; r.amount = n; return r; }
    public static Reward cosmetic(String id) { Reward r = new Reward(); r.type = Type.COSMETIC; r.cosmeticId = id; return r; }
    public static Reward key(String crate, int n) { Reward r = new Reward(); r.type = Type.KEY; r.crateId = crate; r.amount = n; return r; }
    public static Reward xp(int n) { Reward r = new Reward(); r.type = Type.XP; r.amount = n; return r; }
    public static Reward item(ItemStack it) { Reward r = new Reward(); r.type = Type.ITEM; r.item = it.clone(); r.amount = it.getAmount(); return r; }
    public static Reward command(String cmd, String display) { Reward r = new Reward(); r.type = Type.COMMAND; r.command = cmd; r.display = display; return r; }

    public Reward copy() {
        Reward r = new Reward();
        r.type = type; r.amount = amount; r.item = item == null ? null : item.clone();
        r.cosmeticId = cosmeticId; r.crateId = crateId; r.command = command; r.display = display;
        return r;
    }

    public static Reward load(ConfigurationSection s) {
        if (s == null) return null;
        Reward r = new Reward();
        try { r.type = Type.valueOf(s.getString("type", "CORONA").toUpperCase()); } catch (Exception e) { return null; }
        r.amount = Math.max(1, s.getInt("amount", 1));
        r.cosmeticId = s.getString("cosmetic");
        r.crateId = s.getString("crate");
        r.command = s.getString("command");
        r.display = s.getString("display");
        if (r.type == Type.ITEM) {
            r.item = s.getItemStack("item");
            if (r.item == null) {
                Material m = Material.matchMaterial(s.getString("material", "STONE"));
                r.item = new ItemStack(m == null ? Material.STONE : m);
            }
            r.item.setAmount(Math.min(r.item.getMaxStackSize(), r.amount));
        }
        return r;
    }

    public void save(ConfigurationSection s) {
        s.set("type", type.name());
        s.set("amount", amount);
        if (cosmeticId != null) s.set("cosmetic", cosmeticId);
        if (crateId != null) s.set("crate", crateId);
        if (command != null) s.set("command", command);
        if (display != null) s.set("display", display);
        if (item != null) s.set("item", item);
    }

    /** Serializacion de una linea para la cola de pendientes de PlayerData. */
    public String serialize() {
        YamlConfiguration y = new YamlConfiguration();
        save(y);
        return y.saveToString();
    }
    public static Reward deserialize(String s) {
        YamlConfiguration y = new YamlConfiguration();
        try { y.loadFromString(s); } catch (Exception e) { return null; }
        return load(y);
    }

    public String describe(AetheriaRewards plugin) {
        switch (type) {
            case CORONA: return "§6" + amount + " Corona" + (amount == 1 ? "" : "s") + " Real" + (amount == 1 ? "" : "es");
            case XP: return "§b" + amount + " XP de pase";
            case KEY: return "§e" + amount + "× " + plugin.crates().keyName(crateId);
            case COSMETIC: {
                CosmeticDef d = plugin.cosmetics().get(cosmeticId);
                return d == null ? "§c(cosmético desconocido: " + cosmeticId + ")" : d.rarity.color + d.name + " §7(cosmético)";
            }
            case COMMAND: return "§a" + (display == null ? "Recompensa especial" : display);
            default:
                if (item == null) return "§cobjeto inválido";
                String n = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? Util.plain(item.getItemMeta().displayName()) : "§f" + item.getType().name();
                return n + " §7×" + amount;
        }
    }

    public ItemStack icon(AetheriaRewards plugin) {
        switch (type) {
            case CORONA: { ItemStack c = Currency.apiCoin(amount); return c; }
            case XP: return Util.item(Material.EXPERIENCE_BOTTLE, "§b" + amount + " XP de pase");
            case KEY: return plugin.crates().keyItem(crateId, Math.min(64, amount));
            case COSMETIC: {
                CosmeticDef d = plugin.cosmetics().get(cosmeticId);
                return d == null ? Util.item(Material.BARRIER, "§cDesconocido") : plugin.cosmetics().icon(d, null);
            }
            case COMMAND: return Util.glow(Util.item(Material.NETHER_STAR, "§a" + (display == null ? "Recompensa especial" : display)));
            default:
                if (item == null) return Util.item(Material.BARRIER, "§cInválido");
                ItemStack c = item.clone(); c.setAmount(Math.max(1, Math.min(c.getMaxStackSize(), amount))); return c;
        }
    }

    public List<String> loreLines(AetheriaRewards plugin) {
        List<String> l = new ArrayList<>();
        l.add(describe(plugin));
        return l;
    }
}
