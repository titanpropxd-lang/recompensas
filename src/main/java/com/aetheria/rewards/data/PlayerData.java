package com.aetheria.rewards.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/** Datos persistentes de un jugador. Todo acceso ocurre en el hilo principal. */
public class PlayerData {
    public final UUID uuid;
    public String name = "";
    public final Set<String> unlocked = new HashSet<>();
    public final Set<String> favorites = new HashSet<>();
    public final Map<String, String> equipped = new HashMap<>();      // slot -> cosmetic id
    public long bpXp = 0;
    public String bpSeason = "";
    public final Set<Integer> bpClaimedFree = new HashSet<>();
    public final Set<Integer> bpClaimedPremium = new HashSet<>();
    public final Map<String, Integer> quests = new HashMap<>();       // quest id -> progreso
    public final Set<String> questsClaimed = new HashSet<>();
    public String dailyKey = "", weeklyKey = "";
    public final Set<Integer> adventClaimed = new HashSet<>();
    public final Set<String> februaryClaimed = new HashSet<>();
    public final Set<String> foundChests = new HashSet<>();
    public final Set<String> pendingRewards = new LinkedHashSet<>();   // recompensas serializadas por entregar
    public boolean dirty = false;

    public PlayerData(UUID uuid) { this.uuid = uuid; }

    public static PlayerData load(UUID uuid, YamlConfiguration y) {
        PlayerData d = new PlayerData(uuid);
        d.name = y.getString("name", "");
        d.unlocked.addAll(y.getStringList("unlocked"));
        d.favorites.addAll(y.getStringList("favorites"));
        ConfigurationSection eq = y.getConfigurationSection("equipped");
        if (eq != null) for (String k : eq.getKeys(false)) d.equipped.put(k, eq.getString(k));
        d.bpXp = y.getLong("bp.xp", 0);
        d.bpSeason = y.getString("bp.season", "");
        d.bpClaimedFree.addAll(y.getIntegerList("bp.claimed-free"));
        d.bpClaimedPremium.addAll(y.getIntegerList("bp.claimed-premium"));
        ConfigurationSection q = y.getConfigurationSection("quests.progress");
        if (q != null) for (String k : q.getKeys(false)) d.quests.put(k, q.getInt(k));
        d.questsClaimed.addAll(y.getStringList("quests.claimed"));
        d.dailyKey = y.getString("quests.daily-key", "");
        d.weeklyKey = y.getString("quests.weekly-key", "");
        d.adventClaimed.addAll(y.getIntegerList("advent.claimed"));
        d.februaryClaimed.addAll(y.getStringList("february.claimed"));
        d.foundChests.addAll(y.getStringList("found-chests"));
        d.pendingRewards.addAll(y.getStringList("pending"));
        return d;
    }

    public String serialize() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("unlocked", new ArrayList<>(unlocked));
        y.set("favorites", new ArrayList<>(favorites));
        for (Map.Entry<String, String> e : equipped.entrySet()) y.set("equipped." + e.getKey(), e.getValue());
        y.set("bp.xp", bpXp);
        y.set("bp.season", bpSeason);
        y.set("bp.claimed-free", new ArrayList<>(bpClaimedFree));
        y.set("bp.claimed-premium", new ArrayList<>(bpClaimedPremium));
        for (Map.Entry<String, Integer> e : quests.entrySet()) y.set("quests.progress." + e.getKey(), e.getValue());
        y.set("quests.claimed", new ArrayList<>(questsClaimed));
        y.set("quests.daily-key", dailyKey);
        y.set("quests.weekly-key", weeklyKey);
        y.set("advent.claimed", new ArrayList<>(adventClaimed));
        y.set("february.claimed", new ArrayList<>(februaryClaimed));
        y.set("found-chests", new ArrayList<>(foundChests));
        y.set("pending", new ArrayList<>(pendingRewards));
        return y.saveToString();
    }
}
